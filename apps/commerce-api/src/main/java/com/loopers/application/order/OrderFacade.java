package com.loopers.application.order;

import com.loopers.application.coupon.CouponService;
import com.loopers.application.order.OrderCommand.OrderItemRequest;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;
    private final PointService pointService;
    private final CouponService couponService;

    @Transactional
    public OrderInfo createOrder(String userId, OrderCommand.Create command) {
        // 1. 쿠폰 검증 및 사용 처리
        UserCoupon userCoupon = validateAndUseCoupon(userId, command.userCouponId());

        // 2. 주문 생성 및 상품 추가
        Order order = createOrderWithItems(userId, command.orderItems());

        // 3. 최종 결제 금액 계산
        BigDecimal finalAmount = calculateFinalAmount(order, userCoupon);

        // 4. 포인트 차감
        deductPoint(userId, finalAmount);

        // 5. 주문 저장
        Order savedOrder = orderRepository.save(order);

        return OrderInfo.from(savedOrder);
    }

    private UserCoupon validateAndUseCoupon(String userId, Long userCouponId) {
        if (userCouponId == null) {
            return null;
        }

        UserCoupon userCoupon = couponService.getUserCouponWithLock(userCouponId);
        userCoupon.useBy(userId); // 도메인이 검증과 사용 처리를 담당
        return userCoupon;
    }

    private Order createOrderWithItems(String userId, List<OrderItemRequest> orderItemRequests) {
        // 상품 조회 (비관적 락)
        Map<Long, Product> productMap = loadProductsWithLock(orderItemRequests);

        // 주문 생성
        Order order = Order.builder()
            .userId(userId)
            .status(OrderStatus.PENDING)
            .build();

        // 주문 항목 추가 및 재고 차감
        for (OrderItemRequest request : orderItemRequests) {
            Product product = productMap.get(request.productId());
            product.deductStock(request.quantity());
            order.addOrderItem(OrderItem.from(product, request.quantity()));
        }

        order.calculateTotalAmount();
        return order;
    }

    private Map<Long, Product> loadProductsWithLock(List<OrderItemRequest> orderItemRequests) {
        List<Long> productIds = orderItemRequests.stream()
            .map(OrderItemRequest::productId)
            .distinct()
            .toList();

        return productIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> productRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "존재하지 않는 상품이 포함되어 있습니다."))
            ));
    }

    private BigDecimal calculateFinalAmount(Order order, UserCoupon userCoupon) {
        if (userCoupon == null) {
            return order.getTotalAmount();
        }

        BigDecimal discountAmount = userCoupon.calculateDiscount(order.getTotalAmount());
        return order.applyDiscount(discountAmount);
    }

    private void deductPoint(String userId, BigDecimal amount) {
        Point point = pointRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));
        point.use(amount);
    }

    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        // 권한 확인
        if (!order.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "권한이 없습니다.");
        }

        // 취소 가능 여부 확인 (Order가 검증 수행)
        order.cancel();

        // 재고 복구 (productId로 Product 조회)
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
            product.restoreStock(orderItem.getQuantity());
        }

        // 포인트 환불
        pointService.refundPoint(userId, order.getTotalAmount());
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderInfo> getOrdersByUser(String userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(OrderInfo::from);
    }
}
