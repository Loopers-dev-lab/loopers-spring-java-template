package com.loopers.core.domain.order.vo;

import java.util.Objects;

public record CouponId(String value) {

    public static CouponId empty() {
        return new CouponId(null);
    }

    public boolean nonEmpty() {
        return Objects.nonNull(value);
    }
}
