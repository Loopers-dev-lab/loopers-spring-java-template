
package com.loopers.domain.coupon;

import com.loopers.application.event.BusinessActionEvent;
import com.loopers.domain.order.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CouponService {

  private final CouponRepository couponRepository;
  private final CouponIssueRepository couponIssueRepository;
  private final ApplicationEventPublisher eventPublisher;

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
  public Money useCouponById(Long issueId, Long userId, Money totalPrice) {
    CouponIssue issue = couponIssueRepository.findById(issueId)
        .filter(i -> i.getUserId().equals(userId))
        .orElseThrow(() -> new IllegalArgumentException("쿠폰 없음"));

    if (!issue.canUse()) {
      throw new IllegalStateException("사용 불가 쿠폰");
    }

    Money discountedPrice = issue.getCoupon().discount(totalPrice);
    BigDecimal discountAmount = totalPrice.getAmount().subtract(discountedPrice.getAmount());

    issue.markUsed();
    issue.getCoupon().increaseUsed();
    couponIssueRepository.save(issue);
    couponRepository.save(issue.getCoupon());
    publishCouponUsedEvent(userId, issue.getCoupon().getId(), totalPrice.getAmount(), discountAmount);

    return discountedPrice;
  }

  @Transactional
  public Money useCouponByCode(String couponCode, Long userId, Money totalPrice) {
    CouponIssue issue = couponIssueRepository.findByCouponCodeAndUserId(couponCode, userId)
        .orElseThrow(() -> new IllegalArgumentException("쿠폰 없음"));

    if (!issue.canUse()) {
      throw new IllegalStateException("사용 불가 쿠폰");
    }

    Money discountedPrice = issue.getCoupon().discount(totalPrice);
    BigDecimal discountAmount = totalPrice.getAmount().subtract(discountedPrice.getAmount());

    issue.markUsed();
    issue.getCoupon().increaseUsed();

    publishCouponUsedEvent(userId, issue.getCoupon().getId(), totalPrice.getAmount(), discountAmount);

    return discountedPrice;
  }

  @Transactional(readOnly = true)
  public List<CouponIssue> getMyCoupons(Long userId) {
    return couponIssueRepository.findAllByUserId(userId);
  }

  @Transactional
  public void rollbackCouponUsage(Long couponIssueId) {
    if (couponIssueId == null) {
      return;
    }

    CouponIssue issue = couponIssueRepository.findById(couponIssueId)
        .orElse(null);

    if (issue != null && !issue.canUse()) {
      issue.markUnused();
      issue.getCoupon().decreaseUsed();
      couponIssueRepository.save(issue);
    }
  }

  private void publishCouponUsedEvent(Long userId, Long couponId, BigDecimal originalAmount, BigDecimal discountAmount) {
    try {
      BusinessActionEvent event = BusinessActionEvent.couponUsed(userId, couponId, null, originalAmount, discountAmount);
      eventPublisher.publishEvent(event);
    } catch (Exception e) {

    }
  }
}
