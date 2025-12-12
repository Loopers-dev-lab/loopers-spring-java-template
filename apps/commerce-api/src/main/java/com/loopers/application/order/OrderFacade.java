package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public OrderInfo createOrder(OrderPlaceCommand command) {
        log.info("주문 생성 시작: userBusinessId={}, items={}",
                command.userId(), command.items().size());

        // 1. 사용자 조회
        User user = userService.getUserByUserId(command.userId());

        // 2. 상품 조회 (데드락 방지를 위한 정렬 + 비관적 락)
        List<Long> productIds = extractAndSortProductIds(command.items());
        List<Product> products = productService.getProductsByIdsWithPessimisticLock(productIds);
        Map<Long, Product> productMap = toProductMap(products);

        // 3. 재고 검증 및 차감 (도메인 로직은 Product가 처리)
        validateAndDecreaseStock(command.items(), productMap);

        // 4. 주문 생성 (도메인 서비스 위임)
        List<OrderService.OrderItemRequest> itemRequests = command.items().stream()
                .map(item -> OrderService.OrderItemRequest.of(item.productId(), item.quantity()))
                .toList();
        Order order = orderService.createOrderWithItems(user, itemRequests, productMap);

        // 5. 쿠폰 적용 (선택)
        Long discountAmount = applyCouponIfExists(command.couponId(), user, order);

        // 6. 주문 저장
        Order savedOrder = orderService.save(order);

        log.info("주문 생성 완료: orderId={}, totalAmount={}, discountAmount={}",
                savedOrder.getId(), savedOrder.getTotalAmountValue(), discountAmount);

        return OrderInfo.from(savedOrder, discountAmount);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long couponId) {
        log.info("주문 취소 시작: orderId={}", orderId);

        Order order = orderService.getOrderById(orderId);

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

    @Transactional
    public void completeOrder(Long orderId) {
        log.info("주문 완료 처리: orderId={}", orderId);

        Order order = orderService.getOrderById(orderId);
        order.completePayment();
        orderService.save(order);

        log.info("주문 완료: orderId={}", orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getMyOrders(String userId) {
        User user = userService.getUserByUserId(userId);
        List<Order> orders = orderService.getOrdersByUser(user);

        return orders.stream()
                .map(order -> OrderInfo.from(order, 0L))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrderDetail(Long orderId, String userId) {
        User user = userService.getUserByUserId(userId);
        Order order = orderService.getOrderByIdAndUser(orderId, user);

        return OrderInfo.from(order, 0L);
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

    /**
     * 쿠폰 적용
     */
    private Long applyCouponIfExists(Long couponId, User user, Order order) {
        if (couponId == null) {
            return 0L;
        }

        Coupon coupon = couponService.getCouponWithOptimisticLock(couponId);
        couponService.validateCouponUsable(coupon, user);

        Long discountAmount = coupon.calculateDiscount(order.getTotalAmountValue());
        coupon.use();
        couponService.save(coupon);

        order.applyCoupon(couponId);

        log.info("쿠폰 적용 완료: couponId={}, discountAmount={}", couponId, discountAmount);

        return discountAmount;
    }

    /**
     * 쿠폰 복구
     */
    private void restoreCoupon(Long couponId) {
        Coupon coupon = couponService.getCouponWithOptimisticLock(couponId);
        coupon.restore();
        couponService.save(coupon);
        log.debug("쿠폰 복구: couponId={}", couponId);
    }

    /**
     * 상품 ID 추출 및 정렬
     */
    private List<Long> extractAndSortProductIds(
            List<OrderPlaceCommand.OrderItemCommand> items
    ) {
        return items.stream()
                .map(OrderPlaceCommand.OrderItemCommand::productId)
                .sorted()
                .toList();
    }

    /**
     * 상품 리스트를 Map으로 변환
     */
    private Map<Long, Product> toProductMap(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
