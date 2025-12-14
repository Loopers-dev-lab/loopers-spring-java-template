package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

  @Column(name = "ref_user_id", nullable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ref_coupon_policy_id", nullable = false)
  private CouponPolicy couponPolicy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CouponStatus status;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "ref_used_order_id")
  private Long usedOrderId;

  private Coupon(Long userId, CouponPolicy couponPolicy) {
    validateUserId(userId);
    validateCouponPolicy(couponPolicy);
    this.userId = userId;
    this.couponPolicy = couponPolicy;
    this.status = CouponStatus.AVAILABLE;
  }

  public static Coupon of(Long userId, CouponPolicy couponPolicy) {
    return new Coupon(userId, couponPolicy);
  }

  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.INVALID_COUPON_USER_EMPTY);
    }
  }

  private void validateCouponPolicy(CouponPolicy couponPolicy) {
    if (couponPolicy == null) {
      throw new CoreException(ErrorType.INVALID_COUPON_POLICY_EMPTY);
    }
  }

  public Money calculateDiscount(Money orderAmount) {
    return couponPolicy.calculateDiscount(orderAmount);
  }

  public void toUsed(Long orderId, LocalDateTime usedAt) {
    Objects.requireNonNull(orderId, "주문 ID는 null일 수 없습니다.");
    Objects.requireNonNull(usedAt, "사용 시각은 null일 수 없습니다.");

    if (this.status != CouponStatus.AVAILABLE) {
      throw new CoreException(ErrorType.COUPON_ALREADY_USED);
    }

    this.status = CouponStatus.USED;
    this.usedAt = usedAt;
    this.usedOrderId = orderId;
  }

  public void toAvailable() {
    if (this.status != CouponStatus.USED) {
      throw new CoreException(ErrorType.COUPON_NOT_USED);
    }

    this.status = CouponStatus.AVAILABLE;
    this.usedAt = null;
    this.usedOrderId = null;
  }

  public boolean isOwnedBy(Long userId) {
    return Objects.equals(this.userId, userId);
  }

  public boolean isAvailable() {
    return this.status == CouponStatus.AVAILABLE;
  }

  public Long getUserId() {
    return userId;
  }

  public CouponPolicy getCouponPolicy() {
    return couponPolicy;
  }

  public CouponStatus getStatus() {
    return status;
  }

  public LocalDateTime getUsedAt() {
    return usedAt;
  }

  public Long getUsedOrderId() {
    return usedOrderId;
  }
}
