package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

  @Column(name = "ref_user_id", nullable = false)
  private long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CouponType type;

  private long discountValue;

  private boolean used;

  public Coupon(long userId, CouponType type, long discountValue) {
    if (type == CouponType.PERCENTAGE && (discountValue < 0 || discountValue > 100)) {
      throw new CoreException(ErrorType.BAD_REQUEST, "할인율은 0~100% 사이여야 합니다.");
    }
    this.userId = userId;
    this.type = type;
    this.discountValue = discountValue;
    this.used = false;
  }

  public boolean isUsed() {
    return used;
  }

  public long calculateDiscountAmount(long totalOrderAmount) {
    return this.type.calculate(totalOrderAmount, this.discountValue);
  }

  public void use() {
    if (this.used) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
    }
    this.used = true;
  }

}
