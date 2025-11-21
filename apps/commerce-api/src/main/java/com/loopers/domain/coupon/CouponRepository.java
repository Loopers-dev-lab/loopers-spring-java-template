package com.loopers.domain.coupon;

import com.loopers.domain.user.User;

import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long id);
    Optional<Coupon> findByIdWithOptimisticLock(Long id);
    Optional<Coupon> findByIdWithPessimisticLock(Long id);
    Optional<Coupon> findByIdAndUser(Long id, User user);
}
