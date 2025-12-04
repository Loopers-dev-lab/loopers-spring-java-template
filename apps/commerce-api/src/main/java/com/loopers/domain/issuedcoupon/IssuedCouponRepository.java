package com.loopers.domain.issuedcoupon;

import java.util.Optional;

public interface IssuedCouponRepository {
    IssuedCoupon save(IssuedCoupon issuedCoupon);

    Optional<IssuedCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
