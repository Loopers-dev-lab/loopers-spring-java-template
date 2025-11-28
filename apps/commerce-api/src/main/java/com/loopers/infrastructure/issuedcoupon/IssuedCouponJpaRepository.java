package com.loopers.infrastructure.issuedcoupon;

import com.loopers.domain.issuedcoupon.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
}
