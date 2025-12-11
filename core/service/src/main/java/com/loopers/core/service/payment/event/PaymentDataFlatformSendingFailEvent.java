package com.loopers.core.service.payment.event;

import com.loopers.core.domain.payment.vo.PaymentId;

public record PaymentDataFlatformSendingFailEvent(
        PaymentId paymentId,
        boolean retryable,
        int retryCount,
        String message
) {

    public static PaymentDataFlatformSendingFailEvent create(
            PaymentId paymentId,
            boolean retryable,
            String message) {

        return new PaymentDataFlatformSendingFailEvent(paymentId, retryable, 0, message);
    }
}
