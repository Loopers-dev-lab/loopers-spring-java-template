package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponRepository {

  Coupon save(Coupon coupon);

  Optional<Coupon> findById(Long id);
}
