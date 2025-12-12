package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;

public record CouponInfo(Long id, Long userId, CouponType couponType, int quantity) {
    public static CouponInfo from(Coupon coupon) {
        return new CouponInfo(
                coupon.getId(),
                coupon.getUserId(),
                coupon.getCouponType(),
                coupon.getQuantity()
        );
    }
}
