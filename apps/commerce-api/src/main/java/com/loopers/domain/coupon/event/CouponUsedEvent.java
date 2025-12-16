package com.loopers.domain.coupon.event;

import com.loopers.domain.Money;

public record CouponUsedEvent(
        Long userId,
        Long couponId,
        Long orderId,
        Money discountAmount
) {
    public static CouponUsedEvent of(
            Long userId,
            Long couponId,
            Long orderId,
            Money discountAmount
    ) {
        return new CouponUsedEvent(userId, couponId, orderId, discountAmount);
    }
}
