package com.loopers.application.order;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.CommercePayment;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PgFeignClient;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.util.IdempotencyService;
import com.loopers.support.util.IdempotencyType;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class);

    private final OrderService orderService;
    private final ProductService productService;
    private final StockService stockService;
//    private final PointService pointService;
    private final PgFeignClient pgFeignClient;
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @Value("${pg.api.callbackUrl}")
    private String pgCallbackUrl;

    /**
     * 주문 생성
     */
    @Transactional
    public void createOrder(Long userId, OrderDto.CreateOrderRequest request) {
        log.info("=== 주문 생성 시작 ===");
        log.info("userId: {}, request: {}", userId, request);

        // 0. 주문 요청 유효성 검사
        log.info("[단계 0] 주문 요청 유효성 검사 시작");
        request.validate();
        log.info("[단계 0] 주문 요청 유효성 검사 완료");

        // 1. 멱등성 키 체크
        log.info("[단계 1] 멱등성 키 체크 시작");
        String idempotencyKey = request.generateIdempotentKey(userId);
        log.info("멱등성 키: {}", idempotencyKey);
        if (idempotencyService.checkAndSet(IdempotencyType.ORDER, idempotencyKey)) {
            log.warn("이미 처리된 주문입니다. idempotencyKey: {}", idempotencyKey);
            throw new CoreException(
                ErrorType.CONFLICT,
                "이미 주문이 처리되었습니다."
            );
        }
        log.info("[단계 1] 멱등성 키 체크 완료");

        // 2. 주문 항목 리스트 조회
        log.info("[단계 2] 주문 항목 리스트 조회");
        List<OrderDto.OrderItemRequest> orderItemRequests = request.items();
        log.info("주문 항목 개수: {}", orderItemRequests.size());

        // 3. Product 조회 및 재고 차감
        log.info("[단계 3] Product 조회 및 재고 차감 시작");
        Map<Long, Product> productMap = new HashMap<>();
        orderItemRequests.stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId))  // productId로 정렬 (데드락 방지)
                .forEach(itemRequest -> {
                    log.info("Product 조회 시도 - productId: {}, quantity: {}", itemRequest.productId(), itemRequest.quantity());
                    // 3-1. Product 객체 조회
                    Product product = productService.findById(itemRequest.productId());
                    log.info("Product 조회 성공 - productId: {}, productName: {}", product.getId(), product.getName());
                    productMap.put(itemRequest.productId(), product);

                    log.info("재고 차감 시도 - productId: {}, quantity: {}", itemRequest.productId(), itemRequest.quantity());
                    // 3-2. Stock(재고) 차감, 만약 재고가 부족하면 예외 발생
                    stockService.decreaseQuantity(itemRequest.productId(), Long.valueOf(itemRequest.quantity()));
                    log.info("재고 차감 성공 - productId: {}", itemRequest.productId());
                });
        log.info("[단계 3] Product 조회 및 재고 차감 완료");

        // 4. 주문 생성
        log.info("[단계 4] 주문 생성 시작");
        Order order = Order.builder()
                .discountAmount(BigDecimal.ZERO)  // 초기 할인 금액은 0 (쿠폰 적용 전)
                .shippingFee(BigDecimal.ZERO)     // 배송비는 기본값 0 (필요시 비즈니스 로직으로 계산)
                .userId(userId)
                .build();
        
        // 4-1. OrderItem 리스트 생성 및 적용 (Order를 통해서만 생성)
        // 주문 시점의 상품 정보를 스냅샷으로 저장
        orderItemRequests.forEach(itemRequest -> {
            Product product = productMap.get(itemRequest.productId());
            order.addOrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    itemRequest.quantity()
            );
        });

        // 4-2. 주문 먼저 저장 (쿠폰 적용을 위해 order.getId()가 필요)
        log.info("주문 저장 시도");
        Order savedOrder = orderService.saveOrder(order);
        log.info("주문 저장 성공 - orderId: {}", savedOrder.getId());
        log.info("[단계 4] 주문 생성 완료");

        // 5. 쿠폰 적용 (포인트 차감 전에 할인 적용)
        log.info("[단계 5] 쿠폰 적용 시작");
        if (request.couponIds() != null && !request.couponIds().isEmpty()) {
            log.info("쿠폰 ID 목록: {}", request.couponIds());
                for(Long couponId : request.couponIds()) {
                    log.info("쿠폰 적용 시도 - couponId: {}", couponId);
                        couponService.useCoupon(savedOrder, couponId);
                    log.info("쿠폰 적용 성공 - couponId: {}", couponId);
                }
        } else {
            log.info("적용할 쿠폰이 없습니다.");
        }
        log.info("[단계 5] 쿠폰 적용 완료");

        // 6. 포인트 차감 (쿠폰 할인 적용 후 최종 금액으로)

        // 만약 최종 금액이 0보다 작으면 포인트 차감하지 않음
        BigDecimal finalAmount = savedOrder.getFinalAmount();
        ApiResponse<PaymentDto.PgResponse> pgApiResponse = null;
        PaymentDto.PgResponse pgResponse = null;
        CommercePayment commercePayment = null;

        try {
            if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
                // pointService.deduct(userId, finalAmount);
                log.info("PG 결제 요청 시도 - userId: {}, finalAmount: {}", userId, finalAmount);
                pgApiResponse = pgFeignClient.approvePayment(
                        userId,
                        PaymentDto.PgRequest.builder()
                                .orderId(savedOrder.getOrderIdAsString())
                                .cardNo("1111-2222-3333-4444")
                                .cardType(PaymentDto.CardType.SAMSUNG)
                                .amount(finalAmount.longValue())
                                .callbackUrl(pgCallbackUrl)
                                .build()
                );
                
                // ApiResponse에서 data 추출
                if (pgApiResponse != null && pgApiResponse.data() != null) {
                    pgResponse = pgApiResponse.data();
                    log.info("PG 결제 요청 성공 - transactionKey: {}, status: {}", pgResponse.transactionKey(), pgResponse.status());
                } else {
                    log.warn("PG 결제 응답의 data가 null입니다 - apiResponse: {}", pgApiResponse);
                }
            }
        } catch (Exception e) {
            log.error("PG 결제 요청 중 예외 발생: {}", e.getMessage(), e);
            // 예외 발생 시 pgResponse는 null이므로, finally 블록에서 처리
        } finally {
            commercePayment = paymentService.saveCommercePayment(CommercePayment.builder()
                    .orderId(savedOrder.getId())
                    .transactionKey(pgResponse != null ?  pgResponse.transactionKey() : "")
                    .method(PaymentDto.PaymentMethod.CARD)
                    .cardType(PaymentDto.CardType.SAMSUNG)
                    .cardNo("1111-2222-3333-4444")
                    .paymentStatus(pgResponse != null ? pgResponse.status() : PaymentDto.PaymentStatus.FAILED)
                    .amount(finalAmount)
                    .build()
            );
        }

        // 7. 요청 실패 시 500 에러 반환
        log.info("[단계 7] 결제 결과 확인");
        if(pgResponse == null || pgResponse.status() != PaymentDto.PaymentStatus.PENDING) {
            String failureReason = pgResponse != null ? pgResponse.reason() : "결제 요청에 실패했습니다.";
            log.error("결제 실패 - failureReason: {}, pgResponse: {}", failureReason, pgResponse);
            
            // 별도 트랜잭션에서 새로운 객체를 생성하여 실패 정보 저장
            log.info("실패 주문 정보 저장 시작");
            saveFailedOrderAndPaymentInNewTransaction(userId, request, failureReason);
            log.info("실패 주문 정보 저장 완료");

            throw new CoreException(
                ErrorType.INTERNAL_ERROR,
                failureReason
            );
        }

        log.info("=== 주문 생성 완료 - orderId: {} ===", savedOrder.getId());
    }
    
    /**
     * PG 콜백 처리
     */
    @Transactional
    public OrderInfo callbackOrder(OrderDto.PgCallbackRequest request) {

        // 주문 정보 조회
        Order order = orderService.findOrderById(Long.parseLong(request.orderId()));

        // 이미 결제 실패한 주문이면 400 에러 보내기
        if(order.getOrderStatus() == OrderStatus.PAYMENT_FAILED) {
            throw new CoreException(
                ErrorType.BAD_REQUEST,
                "이미 결제 실패한 주문입니다."
            );
        }

        // 결제 정보보 조회
        CommercePayment commercePayment = paymentService.findByTransactionKey(request.transactionKey());

        // 실패하면 원복 처리 및 400 에러 보내기
        if(request.status() == PaymentDto.PaymentStatus.FAILED) {
            
            // 주문 자원(재고, 쿠폰) 원복 처리
            rollbackOrderResources(order, commercePayment, request.reason());

            throw new CoreException(
                    ErrorType.BAD_REQUEST
                    , request.reason()
            );
        }

        // 결제 완료 처리하기
        paymentService.saveSuccessPayment(commercePayment.getTransactionKey());

        // 주문 완료 처리하기
        Order savedOrder = orderService.saveSuccessOrder(order.getId());

        return OrderInfo.from(savedOrder);
    }

    /**
     * 단일 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderInfo getOrder(Long orderId) {
        Order order = orderService.findOrderById(orderId);
        return OrderInfo.from(order);
    }

    /**
     * 유저의 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(Long userId) {
        List<Order> orders = orderService.findOrdersByUserId(userId);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    /**
     * 주문 실패 시 모든 리소스 원복 (별도 트랜잭션)
     * Application Layer에서 도메인 서비스들을 조율하여 원복 처리
     * REQUIRES_NEW로 별도 트랜잭션에서 커밋되므로, 이후 예외 발생과 무관하게 원복이 유지됨
     * 
     * @param order 주문 도메인 모델
     * @param payment 결제 도메인 모델
     * @param reason 실패 사유
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void rollbackOrderResources(Order order, CommercePayment payment, String reason) {
        // 1. 재고 원복 (OrderItem들을 순회하며 재고 복구)
        order.getOrderItems().forEach(orderItem -> {
            stockService.increaseQuantity(
                    orderItem.getProductId(),
                    Long.valueOf(orderItem.getQuantity())
            );
        });

        // 2. 쿠폰 원복 (주문에 사용된 쿠폰들을 미사용 상태로 복구)
        couponService.rollbackCoupon(order.getId());

        // 3. 주문 실패 처리
        orderService.saveFailedOrder(order.getId(), reason);

        // 4. 결제 실패 처리
        paymentService.saveFailedPayment(payment.getTransactionKey(), reason);
    }

    /**
     * 결제 실패 시 주문과 결제 정보를 별도 트랜잭션에서 새로운 객체로 저장
     * 원래 트랜잭션이 롤백되어도 실패 정보가 남도록 함
     * 
     * @param order 원본 주문 도메인 모델
     * @param payment 원본 결제 도메인 모델
     * @param errorMessage 실패 메시지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveFailedOrderAndPaymentInNewTransaction(Long userId, OrderDto.CreateOrderRequest request, String errorMessage) {
        log.info("=== 실패 주문 정보 저장 시작 (REQUIRES_NEW 트랜잭션) ===");
        log.info("userId: {}, errorMessage: {}", userId, errorMessage);
        
        // 1. 새로운 Order 객체 생성 (OrderItem들을 복사)
        Order newOrder = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .userId(userId)
                .build();
        
        // OrderItem들을 복사
        log.info("실패 주문의 OrderItem 복사 시작 - items: {}", request.items());
        request.items().forEach(orderItem -> {
            log.info("실패 주문용 Product 조회 시도 - productId: {}", orderItem.productId());
            Product product = productService.findById(orderItem.productId());
            log.info("실패 주문용 Product 조회 성공 - productId: {}", product.getId());
            newOrder.addOrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    orderItem.quantity()
            );
        });
        
        // 주문 실패 처리
        newOrder.fail(errorMessage);
        
        // Order 저장
        orderService.saveOrder(newOrder);
        
        // 2. 새로운 CommercePayment 객체 생성
        CommercePayment newPayment = CommercePayment.builder()
                .transactionKey("")
                .method(PaymentDto.PaymentMethod.CARD)
                .cardType(PaymentDto.CardType.SAMSUNG)
                .cardNo("1111-2222-3333-4444")
                .orderId(newOrder.getId())
                .amount(newOrder.getFinalAmount())
                .paymentStatus(PaymentDto.PaymentStatus.FAILED)
                .build();
        
        // 결제 실패 처리
        newPayment.fail(errorMessage);
        
        // CommercePayment 저장
        paymentService.saveCommercePayment(newPayment);
    }

}

