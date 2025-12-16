package com.loopers.domain.order.event;

public record OrderFailureEvent(
        Long orderId,
        Long userId,
        String totalAmount,
        String paymentId,
        String paymentType
) {
    public static OrderFailureEvent of(
            Long orderId,
            Long userId,
            String totalAmount,
            String paymentId,
            String paymentType
    ) {
        return new OrderFailureEvent(orderId, userId, totalAmount, paymentId, paymentType);
    }
}
