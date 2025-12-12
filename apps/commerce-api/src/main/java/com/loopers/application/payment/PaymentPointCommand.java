package com.loopers.application.payment;

public record PaymentPointCommand(
        String userId,
        Long orderId,
        Long discountAmount,
        Long couponId,
        String idempotencyKey
) {
    public PaymentPointCommand {
        if (discountAmount == null) {
            discountAmount = 0L;
        }
    }

    public static PaymentPointCommand of(String userId, Long orderId, Long discountAmount,
                                         Long couponId, String idempotencyKey) {
        return new PaymentPointCommand(userId, orderId, discountAmount, couponId, idempotencyKey);
    }
}
