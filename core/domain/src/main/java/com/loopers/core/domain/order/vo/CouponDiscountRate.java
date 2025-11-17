package com.loopers.core.domain.order.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.math.BigDecimal;
import java.util.Objects;

public record CouponDiscountRate(BigDecimal value) {

    private static final BigDecimal MIN_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_RATE = new BigDecimal("100.00");

    public CouponDiscountRate {
        validateNotNull(value);
        validateRange(value);
    }

    private static void validateNotNull(BigDecimal value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("쿠폰 할인율"));
    }

    private static void validateRange(BigDecimal value) {
        if (value.compareTo(MIN_RATE) < 0 || value.compareTo(MAX_RATE) > 0) {
            throw new IllegalArgumentException("할인율은 0~100 사이여야 합니다");
        }
    }
}
