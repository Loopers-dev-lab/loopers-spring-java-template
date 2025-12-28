package com.loopers.application.event;

import com.loopers.domain.order.Money;

public record PaymentFailureEvent(
    String orderId,
    Long userId,
    Money amount,
    String reason
) {
}
