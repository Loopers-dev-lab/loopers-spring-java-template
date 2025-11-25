package com.loopers.core.domain.order.vo;

public record RateDiscountCouponId(String value) {

    public static RateDiscountCouponId empty() {
        return new RateDiscountCouponId(null);
    }
}
