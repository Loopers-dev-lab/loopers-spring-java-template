package com.loopers.application.point;

import com.loopers.domain.order.Money;

public record EarnPointFromPaymentCommand(
    Long userId,
    Money paymentAmount,
    Long orderId
) {
}