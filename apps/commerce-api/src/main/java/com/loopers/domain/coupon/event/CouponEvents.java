package com.loopers.domain.coupon.event;

import com.loopers.domain.stock.event.StockEvents;

import java.math.BigDecimal;

public class CouponEvents {
    
    public record Processed(
        Long orderId,
        Long userId,
        BigDecimal totalDiscountAmount,  // 총 할인 금액
        StockEvents.Processed originalEvent
    ) {}
    
    public record ProcessingFailed(
        Long orderId,
        StockEvents.Processed originalEvent,  // 재고 원복을 위해 필요
        String reason
    ) {}
    
    public record Compensated(
        Long orderId
    ) {}
}

