package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByUserId(String userId);

    List<Coupon> findByUserIdAndStatus(String userId, CouponStatus status);
}
