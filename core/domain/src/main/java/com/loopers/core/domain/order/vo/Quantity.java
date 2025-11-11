package com.loopers.core.domain.order.vo;

import com.loopers.core.domain.error.DomainErrorCode;

public record Quantity(Long value) {

    private static final String FIELD_NAME = "주문 갯수";

    public Quantity {
        validateNegative(value);
    }

    private static void validateNegative(Long value) {
        if (value < 0) {
            throw new IllegalArgumentException(DomainErrorCode.negativeMessage(FIELD_NAME));
        }
    }
}
