package com.loopers.core.domain.payment.vo;

public record PaymentDataPlatformSendingFailEvent(
        PaymentId paymentId,
        boolean retryable,
        int retryCount,
        String message
) {

    public static PaymentDataPlatformSendingFailEvent create(
            PaymentId paymentId,
            boolean retryable,
            String message) {

        return new PaymentDataPlatformSendingFailEvent(paymentId, retryable, 0, message);
    }
}
