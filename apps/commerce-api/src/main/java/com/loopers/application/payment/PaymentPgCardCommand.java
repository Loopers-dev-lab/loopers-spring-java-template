package com.loopers.application.payment;

public record PaymentPgCardCommand(
        String userId,
        Long orderId,
        String cardType,
        String cardNo,
        Long discountAmount,
        Long couponId,
        String idempotencyKey
) {
    public PaymentPgCardCommand {
        if (discountAmount == null) {
            discountAmount = 0L;
        }
    }

    public static PaymentPgCardCommand of(String userId, Long orderId, String cardType,
                                          String cardNo, Long discountAmount, Long couponId,
                                          String idempotencyKey) {
        return new PaymentPgCardCommand(userId, orderId, cardType, cardNo,
                discountAmount, couponId, idempotencyKey);
    }
}

