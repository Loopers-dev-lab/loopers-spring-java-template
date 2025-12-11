package com.loopers.core.service.payment.event;

import com.loopers.core.domain.payment.vo.PaymentId;

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
