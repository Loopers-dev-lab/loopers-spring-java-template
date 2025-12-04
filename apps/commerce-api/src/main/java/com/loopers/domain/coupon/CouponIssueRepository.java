package com.loopers.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponIssueRepository {

  CouponIssue save(CouponIssue couponIssue);

  Optional<CouponIssue> findByCouponCodeAndUserId(String CouponCode, Long userId);

  Optional<CouponIssue> findById(Long id);

  List<CouponIssue> findAllByUserId(Long userId);
}
