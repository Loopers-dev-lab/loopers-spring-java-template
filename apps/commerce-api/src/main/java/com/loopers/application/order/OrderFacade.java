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
        List<OrderItemRequest> orderItemRequests = command.orderItems();
        Long userCouponId = command.userCouponId();

        // 1. 쿠폰 조회 및 검증 (비관적 락 사용)
        UserCoupon userCoupon = null;
        if (userCouponId != null) {
            userCoupon = couponService.getUserCouponWithLock(userCouponId);

            // 쿠폰 소유자 확인
            if (!userCoupon.getUserId().equals(userId)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "본인의 쿠폰만 사용할 수 있습니다.");
            }

            // 쿠폰 사용 가능 여부 확인
            if (!userCoupon.isAvailable()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
            }

            // 쿠폰 사용 처리
            userCoupon.use();
        }

        // 2. 상품 조회 및 재고 확인 (비관적 락 사용)
        List<Long> productIds = orderItemRequests.stream()
            .map(OrderItemRequest::productId)
            .distinct()
            .toList();

        // 비관적 락을 사용하여 상품 조회
        Map<Long, Product> productMap = productIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> productRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "존재하지 않는 상품이 포함되어 있습니다."))
            ));

        // 3. 주문 생성
        Order order = Order.builder()
            .userId(userId)
            .status(OrderStatus.PENDING)
            .build();

        // 4. 주문 항목 생성 및 재고 차감
        for (OrderItemRequest request : orderItemRequests) {
            Product product = productMap.get(request.productId());

            if (!product.isAvailable()) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                    String.format("상품 '%s'은(는) 현재 판매 불가능합니다.", product.getName()));
            }

            // 재고 차감 (Product가 재고 검증 및 차감 수행)
            product.deductStock(request.quantity());

            // 스냅샷 패턴: 주문 당시의 상품 정보를 저장
            OrderItem orderItem = OrderItem.from(product, request.quantity());

            order.addOrderItem(orderItem);
        }

        // 5. 총 금액 계산
        order.calculateTotalAmount();

        // 6. 쿠폰 할인 적용 (쿠폰이 있는 경우)
        BigDecimal finalAmount = order.getTotalAmount();
        if (userCoupon != null) {
            BigDecimal discountAmount = userCoupon.getCoupon().calculateDiscountAmount(order.getTotalAmount());
            finalAmount = order.getTotalAmount().subtract(discountAmount);

            // 할인 후 금액이 0보다 작으면 0으로 설정
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }
        }

        // 7. 포인트 차감 (비관적 락 사용)
        Point point = pointRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));

        // 포인트 사용 (Point 엔티티가 잔액 검증 수행)
        point.use(finalAmount);

        // 8. 주문 저장
        Order savedOrder = orderRepository.save(order);

        return OrderInfo.from(savedOrder);
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
