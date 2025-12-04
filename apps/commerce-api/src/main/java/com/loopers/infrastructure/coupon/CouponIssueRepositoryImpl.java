package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponIssue;
import com.loopers.domain.coupon.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponIssueRepositoryImpl implements CouponIssueRepository {
  private final CouponIssueJpaRepository jpaRepository;

  @Override
  public CouponIssue save(CouponIssue couponIssue) {
    return jpaRepository.save(couponIssue);
  }

  @Override
  public Optional<CouponIssue> findByCouponCodeAndUserId(String couponCode, Long userId) {
    return jpaRepository.findByCouponCodeAndUserId(couponCode, userId);
  }

  @Override
  public Optional<CouponIssue> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public List<CouponIssue> findAllByUserId(Long userId) {
    return jpaRepository.findAllByUserId(userId);
  }
}
