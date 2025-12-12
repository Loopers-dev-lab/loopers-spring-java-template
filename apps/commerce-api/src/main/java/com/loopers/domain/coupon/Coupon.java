package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.CurrencyCode;
import com.loopers.domain.order.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
public class Coupon extends BaseEntity {

  @Enumerated(EnumType.STRING)
  private CouponPolicyType policyType; // AMOUNT, PERCENT

  private BigDecimal discountAmount;
  private Double discountRate;

  private LocalDateTime startAt;
  private LocalDateTime endAt;

  private Long maxUsage;
  private Long usedCount = 0L;

  protected Coupon() {

  }

  private Coupon(
      CouponPolicyType policyType,
      BigDecimal discountAmount,
      Double discountRate,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Long maxUsage
  ) {
    this.policyType = policyType;
    this.discountAmount = discountAmount;
    this.discountRate = discountRate;
    this.startAt = startAt;
    this.endAt = endAt;
    this.maxUsage = maxUsage;
    this.usedCount = 0L;
  }

  public boolean isAvailable() {
    return usedCount < maxUsage && LocalDateTime.now().isBefore(endAt);
  }

  public Money discount(Money price) {
    if (policyType == CouponPolicyType.AMOUNT) {
      // 고정 금액 할인
      Money discountMoney = new Money(discountAmount, CurrencyCode.KRW);
      Money discountedPrice = price.subtract(discountMoney);

      // 음수 방지 - 0원 이하로 내려가지 않도록
      Money zero = Money.of(BigDecimal.ZERO, CurrencyCode.KRW);
      return discountedPrice.getAmount().compareTo(BigDecimal.ZERO) < 0 ? zero : discountedPrice;
    }

    // 비율 할인
    Money discountMoney = price.multiply(discountRate);
    Money discountedPrice = price.subtract(discountMoney);

    // 음수 방지
    Money zero = Money.of(BigDecimal.ZERO, CurrencyCode.KRW);
    return discountedPrice.getAmount().compareTo(BigDecimal.ZERO) < 0 ? zero : discountedPrice;
  }


  public void increaseUsed() {
    if (usedCount >= maxUsage) {
      throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 사용 불가");
    }
    usedCount++;
  }

  public void decreaseUsed() {
    if (usedCount > 0) {
      usedCount--;
    }
  }

  public static Coupon create(CouponPolicyType policyType, BigDecimal discountAmount, Double discountRate, LocalDateTime startAt, LocalDateTime endAt, Long maxUsage) {
    return new Coupon(policyType, discountAmount, discountRate, startAt, endAt, maxUsage);
  }
}
