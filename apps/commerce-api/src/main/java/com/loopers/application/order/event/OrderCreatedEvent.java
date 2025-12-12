package com.loopers.application.order.event;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 이벤트
 * - 주문이 생성된 후 발행되는 이벤트
 * - 쿠폰 사용, 데이터 플랫폼 전송 등 후속 처리를 트리거한다
 */
public record OrderCreatedEvent(
        Long orderId,
        String userId,
        List<OrderItem> orderItems,
        BigDecimal totalAmount,
        BigDecimal couponDiscount,
        int usedPoints,
        BigDecimal finalAmount,
        Long couponId,
        LocalDateTime orderedAt
) {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getOrderItems(),
                order.getTotalAmount(),
                order.getCouponDiscount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getCouponId(),
                order.getOrderedAt()
        );
    }

    /**
     * 쿠폰이 사용되었는지 확인
     */
    public boolean hasCoupon() {
        return couponId != null;
    }
}
