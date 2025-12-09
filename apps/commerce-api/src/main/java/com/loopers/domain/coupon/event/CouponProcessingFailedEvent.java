package com.loopers.domain.coupon.event;

public record CouponProcessingFailedEvent(
    Long orderId,
    String reason
) {
}