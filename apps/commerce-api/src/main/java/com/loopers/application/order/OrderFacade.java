package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCreatedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성
     * - 핵심 트랜잭션: 재고 차감, 주문 저장
     * - 후속 처리(쿠폰 사용, 데이터 플랫폼 전송): 이벤트로 분리
     */
    @Transactional
    public OrderInfo createOrder(OrderPlaceCommand command) {
        // 1. 사용자 조회
        User user = userService.getUserByLoginId(command.loginId());

        // 2. 상품 조회 (데드락 방지를 위해 ID 정렬 후 조회)
        List<Long> productIds = extractAndSortProductIds(command.items());
        List<Product> products = productService.getProductsByIdsWithPessimisticLock(productIds);
        Map<Long, Product> productMap = toProductMap(products);

        // 3. 재고 검증 및 차감
        validateAndDecreaseStock(command.items(), productMap);

        // 4. 쿠폰 검증 (사용은 이벤트로 분리)
        Long discountAmount = 0L;
        if (command.couponId() != null) {
            Coupon coupon = couponService.getCouponWithOptimisticLock(command.couponId());
            couponService.validateCouponUsable(coupon, user);
            discountAmount = coupon.calculateDiscount(calculateTotalAmount(command.items(), productMap));
        }

        // 5. 주문 생성
        List<OrderService.OrderItemRequest> itemRequests = command.items().stream()
                .map(item -> OrderService.OrderItemRequest.of(item.productId(), item.quantity()))
                .toList();
        Order order = orderService.createOrderWithItems(user, itemRequests, productMap);

        // 6. 쿠폰 ID 저장 (실제 사용은 이벤트로)
        if (command.couponId() != null) {
            order.applyCoupon(command.couponId());
        }

        // 7. 주문 저장
        Order savedOrder = orderService.save(order);

        // 8. 이벤트 발행 → 쿠폰 사용, 데이터 플랫폼 전송, 유저 행동 로깅
        eventPublisher.publishEvent(OrderCreatedEvent.from(savedOrder, discountAmount));

        log.info("주문 생성 완료: orderId={}, userId={}, couponId={}",
                savedOrder.getId(), user.getId(), command.couponId());

        return OrderInfo.from(savedOrder, discountAmount);
    }

    /**
     * 쿠폰 사용 처리 - OrderEventListener에서 호출
     */
    @Transactional
    public void useCoupon(Long couponId) {
        log.info("쿠폰 사용 처리: couponId={}", couponId);
        Coupon coupon = couponService.getCouponWithOptimisticLock(couponId);
        if (!coupon.canUse()) {
            log.info("이미 사용된 쿠폰입니다 (멱등성 처리): couponId={}", couponId);
            return;
        }

        coupon.use();
        couponService.save(coupon);
    }

    /**
     * 주문 취소 - PaymentEventListener에서 호출 (보상 트랜잭션)
     */
    @Transactional
    public void cancelOrder(Long orderId, Long couponId) {
        log.info("주문 취소 시작: orderId={}", orderId);

        Order order = orderService.getOrderById(orderId);

        // 이미 취소된 주문은 스킵 (멱등성)
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("이미 취소된 주문입니다: orderId={}", orderId);
            return;
        }

        // 1. 재고 복구
        List<Long> productIds = order.getOrderItems().stream()
                .map(item -> item.getProductId())
                .sorted()
                .toList();
        List<Product> products = productService.getProductsByIdsWithPessimisticLock(productIds);
        Map<Long, Product> productMap = toProductMap(products);

        orderService.restoreStock(order, productMap);

        // 2. 쿠폰 복구
        Long couponIdToRestore = couponId != null ? couponId : order.getCouponId();
        if (couponIdToRestore != null) {
            restoreCoupon(couponIdToRestore);
        }

        // 3. 주문 취소 상태 변경
        order.markAsCancelled();
        orderService.save(order);

        log.info("주문 취소 완료: orderId={}", orderId);
    }

    /**
     * 주문 완료 처리 - PaymentEventListener에서 호출
     */
    @Transactional
    public void completeOrder(Long orderId) {
        log.info("주문 완료 처리: orderId={}", orderId);

        Order order = orderService.getOrderById(orderId);

        // 이미 완료된 주문은 스킵 (멱등성)
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("이미 완료된 주문입니다: orderId={}", orderId);
            return;
        }

        order.markAsCompleted();
        orderService.save(order);

        log.info("주문 완료: orderId={}", orderId);
    }

    /**
     * 주문에서 사용자 ID 조회 (데이터 플랫폼 전송용)
     */
    @Transactional(readOnly = true)
    public Long getUserIdByOrderId(Long orderId) {
        Order order = orderService.getOrderById(orderId);
        return order.getUser().getId();
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getMyOrders(String loginId) {
        User user = userService.getUserByLoginId(loginId);
        List<Order> orders = orderService.getOrdersByUser(user);

        return orders.stream()
                .map(order -> OrderInfo.from(order, 0L))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrderDetail(Long orderId, String loginId) {
        User user = userService.getUserByLoginId(loginId);
        Order order = orderService.getOrderByIdAndUser(orderId, user);

        return OrderInfo.from(order, 0L);
    }

    private Long calculateTotalAmount(List<OrderPlaceCommand.OrderItemCommand> items, Map<Long, Product> productMap) {
        return items.stream()
                .mapToLong(item -> {
                    Product product = productMap.get(item.productId());
                    return product.getPrice().getValue() * item.quantity();
                })
                .sum();
    }

    private void validateAndDecreaseStock(
            List<OrderPlaceCommand.OrderItemCommand> items,
            Map<Long, Product> productMap
    ) {
        for (OrderPlaceCommand.OrderItemCommand item : items) {
            Product product = productMap.get(item.productId());

            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND,
                        "상품을 찾을 수 없습니다: " + item.productId());
            }

            if (!product.isStockAvailable(item.quantity())) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                        String.format("상품 '%s'의 재고가 부족합니다.", product.getName()));
            }

            product.decreaseStock(item.quantity());
        }
    }

    private void restoreCoupon(Long couponId) {
        Coupon coupon = couponService.getCouponWithOptimisticLock(couponId);
        coupon.restore();
        couponService.save(coupon);
        log.debug("쿠폰 복구: couponId={}", couponId);
    }

    private List<Long> extractAndSortProductIds(List<OrderPlaceCommand.OrderItemCommand> items) {
        return items.stream()
                .map(OrderPlaceCommand.OrderItemCommand::productId)
                .sorted()
                .toList();
    }

    private Map<Long, Product> toProductMap(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
