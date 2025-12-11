package com.loopers.core.service.product.event;

import com.loopers.core.domain.payment.vo.PaymentId;

public record StockIncreaseFailEvent(
        PaymentId paymentId,
        String message,
        boolean retryable,
        int retryCount
) {

    public static StockIncreaseFailEvent create(PaymentId paymentId, String message, boolean retryable) {
        return new StockIncreaseFailEvent(paymentId, message, retryable, 0);
    }
}
