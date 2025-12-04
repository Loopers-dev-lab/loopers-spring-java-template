package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupon_issue")
@Getter
public class CouponIssue extends BaseEntity {

  private String couponCode;

  @ManyToOne(fetch = FetchType.LAZY)
  private Coupon coupon;

  private Long userId;

  @Enumerated(EnumType.STRING)
  private CouponIssueStatus status; // UNUSED, USED

  private LocalDateTime assignedAt;
  private LocalDateTime usedAt;

  protected CouponIssue() {
  }

  private CouponIssue(Coupon coupon, Long userId) {
    this.couponCode = UUID.randomUUID().toString();
    this.coupon = coupon;
    this.userId = userId;
    this.status = CouponIssueStatus.UNUSED;
    this.assignedAt = LocalDateTime.now();
  }

  public void markUsed() {
    this.status = CouponIssueStatus.USED;
    this.usedAt = LocalDateTime.now();
  }

  public boolean canUse() {
    return status == CouponIssueStatus.UNUSED && coupon.isAvailable();
  }

  public boolean isUsed() {
    return this.status == CouponIssueStatus.USED;
  }

  public static CouponIssue create(Coupon coupon, Long userId) {
    return new CouponIssue(coupon, userId);
  }
}

