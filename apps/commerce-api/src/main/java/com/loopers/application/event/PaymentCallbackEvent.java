package com.loopers.application.event;

import com.loopers.application.payment.TransactionStatus;

public record PaymentCallbackEvent(
    Long orderId,
    Long amount,
    TransactionStatus status,
    String reason
) {
}
