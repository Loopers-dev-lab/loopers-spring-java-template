package com.loopers.domain.coupon.event;

public record CouponProcessEvent(
    Long orderId,
    boolean isSuccess,
    String reason
) {
    public static CouponProcessEvent success(Long orderId) {
        return new CouponProcessEvent(orderId, true, null);
    }

    public static CouponProcessEvent failure(Long orderId, String reason) {
        return new CouponProcessEvent(orderId, false, reason);
    }
}
