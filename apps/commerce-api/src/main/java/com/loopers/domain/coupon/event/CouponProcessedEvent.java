package com.loopers.domain.coupon.event;

import com.loopers.domain.stock.event.StockProcessedEvent;

public record CouponProcessedEvent(
    Long orderId,
    StockProcessedEvent originalEvent
) {
}
