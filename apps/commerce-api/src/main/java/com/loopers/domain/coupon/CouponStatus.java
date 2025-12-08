package com.loopers.domain.coupon;

public enum CouponStatus {
  AVAILABLE("사용 가능"),
  USED("사용 완료");

  private final String description;

  CouponStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
