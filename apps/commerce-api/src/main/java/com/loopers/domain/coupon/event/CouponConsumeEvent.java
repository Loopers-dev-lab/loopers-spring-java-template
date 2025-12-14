package com.loopers.domain.coupon.event;

/**
 * @author hyunjikoh
 * @since 2025. 12. 9.
 */
public record CouponConsumeEvent(
        Long couponId,
        Long userId,
        Long orderId
) {
}
