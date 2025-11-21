package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> findCouponByIdAndUserId(Long couponId, Long userId);

    void saveCoupon(Coupon coupon);
}

