package com.loopers.domain.coupon;

public enum CouponDiscountType {
  FIXED("정액"),
  RATE("정률");

  private final String description;

  CouponDiscountType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
