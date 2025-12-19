package com.loopers.domain.coupon;

import aj.org.objectweb.asm.commons.Remapper;

import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Optional<Coupon> findByIdAndUserIdForUpdate(Long couponId, Long userId);

    Optional<Coupon> findByIdAndUserId(Long couponId, Long userId);
}
