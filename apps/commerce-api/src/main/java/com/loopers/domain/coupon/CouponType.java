package com.loopers.domain.coupon;

public enum CouponType {
  FIXED_AMOUNT {
    @Override
    public long calculate(long totalAmount, long discountValue) {
      return Math.min(totalAmount, discountValue);
    }
  },
  PERCENTAGE {
    @Override
    public long calculate(long totalAmount, long discountValue) {
      return (long) (totalAmount * (discountValue / 100.0));
    }
  };

  public abstract long calculate(long totalAmount, long discountValue);
}
