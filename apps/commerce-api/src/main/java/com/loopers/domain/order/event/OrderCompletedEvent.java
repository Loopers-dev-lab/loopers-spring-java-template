package com.loopers.domain.order.event;

public record OrderCompletedEvent(
        Long orderId,
        Long userId,
        String totalAmount,
        String paymentId,
        String paymentType
) {
    public static OrderCompletedEvent of(
            Long orderId,
            Long userId,
            String totalAmount,
            String paymentId,
            String paymentType
    ) {
        return new OrderCompletedEvent(orderId, userId, totalAmount, paymentId, paymentType);
    }
}
