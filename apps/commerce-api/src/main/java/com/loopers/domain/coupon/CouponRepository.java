package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponRepository {
    Coupon registerCoupon(Coupon coupon);

    Optional<Coupon> findValidCoupon(Long couponId);
}
