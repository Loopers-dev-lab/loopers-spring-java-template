package com.loopers.application.event;

import com.loopers.domain.order.Money;

public record OrderPaidEvent(
    String orderId,
    Long userId,
    Money totalAmount
) {
}
