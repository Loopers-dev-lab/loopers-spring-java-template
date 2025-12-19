package com.loopers.domain.coupon.event;

import java.time.LocalDateTime;

public class CouponUsedEvent {

    private final Long couponId;
    private final Long orderId;
    private final String userId;
    private final long discountAmount;
    private final LocalDateTime occurredAt;

    private CouponUsedEvent(Long couponId, Long orderId, String userId, long discountAmount) {
        this.couponId = couponId;
        this.orderId = orderId;
        this.userId = userId;
        this.discountAmount = discountAmount;
        this.occurredAt = LocalDateTime.now();
    }

    public static CouponUsedEvent from(Long couponId, Long orderId, String userId, long discountAmount) {
        return new CouponUsedEvent(couponId, orderId, userId, discountAmount);
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
