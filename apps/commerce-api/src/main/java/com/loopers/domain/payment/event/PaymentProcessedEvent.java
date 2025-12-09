package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponProcessedEvent;

public record PaymentProcessedEvent(
    Long orderId,
    CouponProcessedEvent originalEvent
) {
}
