package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> save(Coupon coupon);
    Optional<Coupon> findById(Long couponId);
    List<Coupon> findByUserId(Long userId);
    List<Coupon> findByUserIdAndIsUsedFalse(Long userId);
    long useCoupon(Long couponId, Long orderId, Long userId);
}

