package com.loopers.domain.coupon;

import com.loopers.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> save(Coupon coupon);
    Optional<Coupon> findById(Long couponId);
    List<Coupon> findByUser(User user);
    List<Coupon> findByUserAndIsUsedFalse(User user);
    long useCoupon(Long couponId, Long orderId, Long userId);
}

