package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponPolicyRepository {

  CouponPolicy save(CouponPolicy couponPolicy);

  Optional<CouponPolicy> findById(Long id);
}
