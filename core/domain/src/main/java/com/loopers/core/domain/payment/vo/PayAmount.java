package com.loopers.core.domain.payment.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.math.BigDecimal;

public record PayAmount(BigDecimal value) {

    private static final String FIELD_NAME = "결제 총 금액";

    public PayAmount {
        validateNegative(value);
    }

    private static void validateNegative(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(DomainErrorCode.negativeMessage(FIELD_NAME));
        }
    }
}
