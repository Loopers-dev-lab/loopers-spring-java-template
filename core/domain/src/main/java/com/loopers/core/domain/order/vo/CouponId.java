package com.loopers.core.domain.order.vo;

public record CouponId(String value) {

    public static CouponId empty() {
        return new CouponId(null);
    }
}
