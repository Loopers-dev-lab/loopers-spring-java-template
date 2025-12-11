package com.loopers.core.domain.order.vo;

import com.loopers.core.domain.payment.vo.PaymentId;

public record PayedOrderFailEvent(
        PaymentId paymentId,
        OrderId orderId,
        boolean retryable,
        int retryCount,
        String message
) {
    public static PayedOrderFailEvent create(
            PaymentId paymentId,
            OrderId orderId,
            boolean retryable,
            String message
    ) {
        return new PayedOrderFailEvent(paymentId, orderId, retryable, 0, message);
    }
}
