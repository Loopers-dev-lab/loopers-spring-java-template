package com.loopers.application.order;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentMethod;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.payment.PaymentCallbackUrlGenerator;
import com.loopers.infrastructure.pg.PgPaymentExecutor;
import com.loopers.infrastructure.pg.PgV1Dto;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class OrderFacade {
    private final CouponService couponService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PointService pointService;
    private final SupplyService supplyService;
    private final UserService userService;

    private final PgPaymentExecutor pgPaymentExecutor;
    private final PaymentCallbackUrlGenerator callbackUrlGenerator;

    @Transactional(readOnly = true)
    public OrderInfo getOrderInfo(String userId, Long orderId) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Order order = orderService.getOrderByIdAndUserId(orderId, user.getId());

        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderInfo> getOrderList(String userId, Pageable pageable) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Page<Order> orders = orderService.getOrdersByUserId(user.getId(), pageable);
        return orders.map(OrderInfo::from);
    }

    @Transactional
    public OrderInfo createOrder(String userId, OrderRequest request) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        request.validate();

        Map<Long, Integer> productIdQuantityMap = request.toItemQuantityMap();
        productIdQuantityMap.forEach(supplyService::checkAndDecreaseStock);

        Map<Product, Integer> productQuantityMap = buildProductQuantityMap(productIdQuantityMap);
        Price originalPrice = calculateOriginalPrice(productQuantityMap);
        DiscountResult discountResult = getDiscountResult(request.couponId(), user, originalPrice);

        Order order = orderService.createOrder(productQuantityMap, user.getId(), discountResult, request.paymentMethod());

        if (request.paymentMethod() == PaymentMethod.POINT) {
            return handlePointPayment(order.getId(), user.getId(), discountResult);
        }
        return handlePgPayment(order.getId(), order.getOrderId(), request, discountResult);
    }

    private Map<Product, Integer> buildProductQuantityMap(Map<Long, Integer> productIdQuantityMap) {
        List<Product> products = productService.getProductsByIds(productIdQuantityMap.keySet());
        return products.stream().collect(Collectors.toMap(
                Function.identity(),
                product -> productIdQuantityMap.get(product.getId())
        ));
    }

    private Price calculateOriginalPrice(Map<Product, Integer> productQuantityMap) {
        return productQuantityMap.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(entry.getValue()))
                .reduce(new Price(0), Price::add);
    }

    private OrderInfo handlePointPayment(Long orderId, Long userId, DiscountResult discountResult) {
        pointService.checkAndDeductPoint(userId, discountResult.finalPrice());
        orderService.markPaidByPoint(orderId);
        Order updated = orderService.getOrderById(orderId);
        return OrderInfo.from(updated);
    }

    private OrderInfo handlePgPayment(Long orderPk, String orderPublicId, OrderRequest request, DiscountResult discountResult) {
        String callbackUrl = callbackUrlGenerator.generateCallbackUrl(orderPublicId);
        orderService.setPgPaymentInfo(orderPk, request.paymentRequest().cardType(), request.paymentRequest().cardNo(), callbackUrl);

        PgV1Dto.PgPaymentRequest pgRequest = new PgV1Dto.PgPaymentRequest(
                orderPublicId,
                request.paymentRequest().cardType(),
                request.paymentRequest().cardNo(),
                (long) discountResult.finalPrice().amount(),
                callbackUrl
        );

        sendPgPaymentAsync(orderPk, pgRequest);
        Order updated = orderService.getOrderById(orderPk);
        return OrderInfo.from(updated);
    }

    private void sendPgPaymentAsync(Long orderPk, PgV1Dto.PgPaymentRequest pgRequest) {
        pgPaymentExecutor.requestPaymentAsync(pgRequest)
                .thenAccept(pgResponse -> {
                    orderService.updateStatusByPgResponse(orderPk, pgResponse);
                    log.info("결제 요청 성공 - orderId: {}, pgResponse: {}", orderPk, pgResponse);
                })
                .exceptionally(throwable -> {
                    log.error("결제 요청 실패 - orderId: {}", orderPk, throwable);
                    return null;
                });
    }

    private DiscountResult getDiscountResult(Long couponId, User user, Price originalPrice) {
        if (couponId != null) {
            return couponService.applyCoupon(couponId, user.getId(), originalPrice);
        }
        return new DiscountResult(originalPrice);
    }

    @Transactional
    public void handlePgPaymentCallback(String orderId, PaymentV1Dto.PaymentCallbackRequest request) {
        log.info("콜백 처리 시작 - orderId: {}, transactionKey: {}, status: {}", orderId, request.transactionKey(), request.status());
        // 이미 완료된 주문인지 확인
        Optional<Order> orderOptional = orderService.findByOrderId(orderId);
        if (orderOptional.isEmpty()) {
            log.warn("존재하지 않는 주문에 대한 콜백 - orderId: {}, request: {}", orderId, request);
            return;
        }
        Order order = orderOptional.get();
        if (order.getPaymentMethod() != PaymentMethod.PG) {
            log.warn("PG 콜백이지만 포인트 결제 주문 - orderId: {}", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("이미 결제 완료된 주문 - orderId: {}, request: {}", orderId, request);
            return;
        }
        PgV1Dto.PgTransactionResponse matchedTransaction = null;
        // PG에 상태 재확인 (선택사항: 콜백 검증)
        try {
            log.info("PG 상태 확인 시도 - orderId: {}", orderId);
            PgV1Dto.PgOrderResponse pgOrderResponse = pgPaymentExecutor.getPaymentByOrderIdAsync(orderId).get();
            // fallback이 아닌 실제 응답인지 확인
            if (BooleanUtils.isTrue(pgOrderResponse.isFallback())) {
                log.warn("PG 상태 확인 결과가 fallback 응답 - orderId: {}", orderId);
                return;
            }

            // 리스트에서 현재 받은 callback response의 transactionKey와 status가 일치하는지 확인
            // 최신 순으로 응답하므로, 만약 현재 주문의 트랜잭션 키를 먼저 찾으면 지금 들어온 요청에 대한 처리는 하지 않는다
            matchedTransaction = pgOrderResponse.transactions().stream()
                    .filter(tx -> StringUtils.equalsIgnoreCase(tx.transactionKey(), request.transactionKey()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("PG 상태 확인 실패 - orderId: {}", orderId, e);
            return;
        }

        // 일치하는 거래가 없으면 경고 로그 기록 후 PENDING 상태로 처리
        if (matchedTransaction == null) {
            log.warn("콜백 검증 실패 - 일치하는 거래 없음 - orderId: {}, transactionKey: {}", orderId, request.transactionKey());
            return;
        }

        log.info("PG 상태 확인 성공 - orderId: {}, matched status: {}", orderId, matchedTransaction.status());

        // 결제 상태 업데이트 후 저장
        order.updateOrderStatus(matchedTransaction);
        orderService.save(order);
    }

    @Transactional
    public OrderInfo payOrder(String userId, Long orderId, OrderRequest.PaymentRequest request) {
        // 사용자 및 주문 조회
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        // 주문 검증

        Order order = orderService.getOrderByIdAndUserIdForUpdate(orderId, user.getId());
        if (order.getStatus() == OrderStatus.PAID) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 결제 완료된 주문입니다.");
        }
        if (order.getPaymentMethod() == PaymentMethod.POINT) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 결제 주문은 PG 결제를 사용할 수 없습니다.");
        }
        // 결제 정보 설정
        String callbackUrl = order.getCallbackUrl();
        if (StringUtils.isBlank(callbackUrl)) {
            callbackUrl = callbackUrlGenerator.generateCallbackUrl(order.getOrderId());
        }
        order.setPaymentInfo(
                request.cardType(),
                request.cardNo(),
                callbackUrl
        );
        order = orderService.save(order);  // 결제 정보 저장
        // 비동기로 PG 결제 요청
        PgV1Dto.PgPaymentRequest pgRequest = new PgV1Dto.PgPaymentRequest(
                order.getOrderId(),
                request.cardType(),
                request.cardNo(),
                (long) order.getFinalPrice().amount(),
                callbackUrl  // 서버에서 생성한 콜백 URL
        );

        sendPgPaymentAsync(orderId, pgRequest);

        return OrderInfo.from(order);
    }
}
