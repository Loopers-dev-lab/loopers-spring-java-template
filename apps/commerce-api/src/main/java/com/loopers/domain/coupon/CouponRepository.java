package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Optional<List<Coupon>> findAllByUserId(Long userId);

    Optional<Coupon> findById(Long couponId);
}
