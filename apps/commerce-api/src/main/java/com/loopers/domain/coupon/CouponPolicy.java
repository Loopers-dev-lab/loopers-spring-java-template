package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "coupon_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type", nullable = false)
  private CouponDiscountType discountType;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "discount_amount"))
  private Money discountAmount;

  @Column(name = "discount_rate", precision = 5, scale = 4)
  private BigDecimal discountRate;

  private CouponPolicy(CouponDiscountType discountType, Money discountAmount, BigDecimal discountRate) {
    validateDiscountType(discountType);
    this.discountType = discountType;
    this.discountAmount = discountAmount;
    this.discountRate = discountRate;
  }

  public static CouponPolicy ofFixed(Money discountAmount) {
    Objects.requireNonNull(discountAmount, "정액 할인 금액은 null일 수 없습니다.");
    return new CouponPolicy(CouponDiscountType.FIXED, discountAmount, null);
  }

  public static CouponPolicy ofRate(BigDecimal discountRate) {
    validateDiscountRate(discountRate);
    return new CouponPolicy(CouponDiscountType.RATE, null, discountRate);
  }

  private void validateDiscountType(CouponDiscountType discountType) {
    if (discountType == null) {
      throw new CoreException(ErrorType.INVALID_COUPON_POLICY_DISCOUNT_TYPE_EMPTY);
    }
  }

  private static void validateDiscountRate(BigDecimal discountRate) {
    if (discountRate == null) {
      throw new CoreException(ErrorType.INVALID_COUPON_POLICY_DISCOUNT_RATE_EMPTY);
    }
    if (discountRate.compareTo(BigDecimal.ZERO) <= 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
      throw new CoreException(ErrorType.INVALID_COUPON_POLICY_DISCOUNT_RATE_RANGE);
    }
  }

  public Money calculateDiscount(Money orderAmount) {
    Objects.requireNonNull(orderAmount, "주문 금액은 null일 수 없습니다.");

    if (discountType == CouponDiscountType.FIXED) {
      return calculateFixedDiscount(orderAmount);
    }
    return calculateRateDiscount(orderAmount);
  }

  private Money calculateFixedDiscount(Money orderAmount) {
    long discount = Math.min(discountAmount.getValue(), orderAmount.getValue());
    return Money.of(discount);
  }

  private Money calculateRateDiscount(Money orderAmount) {
    BigDecimal orderValue = BigDecimal.valueOf(orderAmount.getValue());
    BigDecimal discount = orderValue.multiply(discountRate).setScale(0, RoundingMode.DOWN);
    return Money.of(discount.longValue());
  }

  public CouponDiscountType getDiscountType() {
    return discountType;
  }

  public Long getDiscountAmountValue() {
    return discountAmount != null ? discountAmount.getValue() : null;
  }

  public BigDecimal getDiscountRate() {
    return discountRate;
  }
}
