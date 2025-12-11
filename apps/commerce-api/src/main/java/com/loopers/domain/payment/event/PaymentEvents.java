package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;

public class PaymentEvents {
    
    public record Processed(
        Long orderId,
        CouponEvents.Processed originalEvent
    ) {}
    
    public record ProcessingFailed(
        Long orderId,
        String reason
    ) {}
}

