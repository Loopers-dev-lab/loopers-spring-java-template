package com.loopers.core.domain.order.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.math.BigDecimal;
import java.util.Objects;

public record CouponDiscountAmount(BigDecimal value) {

    private static final String FILED_NAME = "쿠폰 할인금액";

    public CouponDiscountAmount {
        validateNotNull(value);
        validateNotNegative(value);
    }

    private static void validateNotNull(BigDecimal value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage(FILED_NAME));
    }

    private static void validateNotNegative(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(DomainErrorCode.negativeMessage(FILED_NAME));
        }
    }
}
