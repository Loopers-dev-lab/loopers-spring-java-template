package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByUserId(Long userId);
    List<Coupon> findByUserIdAndIsUsedFalse(Long userId);
    List<Coupon> findByOrderId(Long orderId);
}

