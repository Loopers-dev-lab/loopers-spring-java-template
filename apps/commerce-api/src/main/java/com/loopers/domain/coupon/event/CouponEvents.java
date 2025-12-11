package com.loopers.domain.coupon.event;

import com.loopers.domain.stock.event.StockEvents;

public class CouponEvents {
    
    public record Processed(
        Long orderId,
        StockEvents.Processed originalEvent
    ) {}
    
    public record ProcessingFailed(
        Long orderId,
        String reason
    ) {}
    
    public record Compensated(
        Long orderId
    ) {}
}

