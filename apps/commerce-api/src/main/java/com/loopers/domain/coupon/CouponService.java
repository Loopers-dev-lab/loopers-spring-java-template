
package com.loopers.domain.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponService {

  private final CouponRepository couponRepository;
  private final CouponIssueRepository couponIssueRepository;

  @Transactional
  public Long assignCoupon(Long couponId, Long userId) {
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new IllegalArgumentException("쿠폰 없음"));

    if (!coupon.isAvailable()) {
      throw new IllegalStateException("발급 불가 쿠폰");
    }

    CouponIssue issue = CouponIssue.create(coupon, userId);
    CouponIssue saved = couponIssueRepository.save(issue);
    return saved.getId();
  }

  @Transactional(readOnly = true)
  public boolean canUseById(Long issueId, Long userId) {
    return couponIssueRepository.findById(issueId)
        .filter(issue -> issue.getUserId().equals(userId))
        .map(CouponIssue::canUse)
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public boolean canUseByCode(String couponCode, Long userId) {
    return couponIssueRepository.findByCouponCodeAndUserId(couponCode, userId)
        .map(CouponIssue::canUse)
        .orElse(false);
  }

  @Transactional
  public BigDecimal useCouponById(Long issueId, Long userId, BigDecimal price) {
    CouponIssue issue = couponIssueRepository.findById(issueId)
        .filter(i -> i.getUserId().equals(userId))
        .orElseThrow(() -> new IllegalArgumentException("쿠폰 없음"));

    if (!issue.canUse()) {
      throw new IllegalStateException("사용 불가 쿠폰");
    }

    BigDecimal discount = issue.getCoupon().discount(price);

    issue.markUsed();
    issue.getCoupon().increaseUsed();

    return discount;
  }

  @Transactional
  public BigDecimal useCouponByCode(String couponCode, Long userId, BigDecimal price) {
    CouponIssue issue = couponIssueRepository.findByCouponCodeAndUserId(couponCode, userId)
        .orElseThrow(() -> new IllegalArgumentException("쿠폰 없음"));

    if (!issue.canUse()) {
      throw new IllegalStateException("사용 불가 쿠폰");
    }

    BigDecimal discount = issue.getCoupon().discount(price);

    issue.markUsed();
    issue.getCoupon().increaseUsed();

    return discount;
  }

  @Transactional(readOnly = true)
  public List<CouponIssue> getMyCoupons(Long userId) {
    return couponIssueRepository.findAllByUserId(userId);
  }
}
