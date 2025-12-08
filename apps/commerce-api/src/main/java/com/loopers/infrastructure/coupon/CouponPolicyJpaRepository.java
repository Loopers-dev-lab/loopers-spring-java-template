package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {
}
