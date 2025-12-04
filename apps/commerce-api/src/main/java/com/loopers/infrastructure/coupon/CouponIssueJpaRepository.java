package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponIssueJpaRepository extends JpaRepository<CouponIssue, Long> {

  Optional<CouponIssue> findByCouponCodeAndUserId(String couponCode, Long userId);

  List<CouponIssue> findAllByUserId(Long userId);
}
