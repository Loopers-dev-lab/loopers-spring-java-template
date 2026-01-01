package com.loopers.application.event;

import com.loopers.domain.order.Money;

public record PaymentSuccessEvent(
    String orderId,
    Long userId,
    Money amount,
    String reason,
    Money totalOrderAmount
) {
}
