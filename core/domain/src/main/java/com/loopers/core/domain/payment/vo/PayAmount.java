package com.loopers.core.domain.payment.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.math.BigDecimal;
import java.util.Objects;

import static com.loopers.core.domain.error.DomainErrorCode.notNullMessage;

public record PayAmount(BigDecimal value) {

    private static final String FIELD_NAME = "결제 총 금액";

    public PayAmount {
        validateNull(value);
        validateNegative(value);
    }

    private static void validateNegative(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(DomainErrorCode.negativeMessage(FIELD_NAME));
        }
    }

    private static void validateNull(BigDecimal value) {
        if (Objects.isNull(value)) {
            throw new NullPointerException(notNullMessage(FIELD_NAME));
        }
    }

    public PayAmount minus(BigDecimal amount) {
        BigDecimal minusValue = this.value.subtract(amount);
        if (minusValue.signum() < 0) {
            return new PayAmount(BigDecimal.ZERO);
        }

        return new PayAmount(minusValue);
    }

    public BigDecimal applyRateDiscount(BigDecimal discountRate) {
        return this.value.multiply(discountRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

}
