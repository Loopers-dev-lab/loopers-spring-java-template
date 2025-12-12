package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;

public class CouponV1Dto {
    public record CouponRequest(
            Long userId,
            CouponType couponType,
            int quantity
    ) {}

    public record CouponResponse(
            Long id,
            Long userId,
            CouponType couponType,
            int quantity
    ) {
        public static CouponResponse from(CouponInfo info) {
            return new CouponResponse(
                    info.id(),
                    info.userId(),
                    info.couponType(),
                    info.quantity()
            );
        }
    }
}
