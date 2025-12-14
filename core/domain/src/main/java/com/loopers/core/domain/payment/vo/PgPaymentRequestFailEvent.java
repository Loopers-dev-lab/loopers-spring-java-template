package com.loopers.core.domain.payment.vo;

public record PgPaymentRequestFailEvent(
        PaymentId paymentId,
        boolean retryable,
        int retryCount,
        String message
) {

    public static PgPaymentRequestFailEvent create(
            PaymentId paymentId,
            boolean retryable,
            String message) {

        return new PgPaymentRequestFailEvent(paymentId, retryable, 0, message);
    }
}
