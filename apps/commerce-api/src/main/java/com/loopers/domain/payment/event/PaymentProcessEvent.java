package com.loopers.domain.payment.event;

public record PaymentProcessEvent(
    Long orderId,
    boolean isSuccess,
    String reason
) {
    public static PaymentProcessEvent success(Long orderId) {
        return new PaymentProcessEvent(orderId, true, null);
    }

    public static PaymentProcessEvent failure(Long orderId, String reason) {
        return new PaymentProcessEvent(orderId, false, reason);
    }
}
