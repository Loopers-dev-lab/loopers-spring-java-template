package com.loopers.domain.order.event;

import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.order.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 생성 이벤트
 * - 주문이 성공적으로 생성되었을 때 발행
 * - 후속 처리: 쿠폰 사용, 결제 요청, 데이터 전송
 */
public record OrderCreatedEvent(
    String eventId,           // 멱등성 보장용 이벤트 ID
    Long orderId,
    String userId,
    BigDecimal totalAmount,
    BigDecimal finalAmount,   // 할인 적용된 최종 금액
    Long userCouponId,        // null 가능
    List<OrderItemInfo> items,
    LocalDateTime createdAt
) {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
            UUID.randomUUID().toString(),
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount(),
            order.getUserCouponId(),
            order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .toList(),
            LocalDateTime.now()
        );
    }

    public boolean hasCoupon() {
        return userCouponId != null;
    }
}
