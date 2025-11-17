package com.loopers.core.domain.order.vo;

public record AmountDiscountCouponId(String value) {

    public static AmountDiscountCouponId empty() {
        return new AmountDiscountCouponId(null);
    }
}
