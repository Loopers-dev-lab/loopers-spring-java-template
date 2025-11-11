package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.order.vo.Quantity;

import java.math.BigDecimal;
import java.util.Objects;

public record ProductPrice(BigDecimal value) {

    private static final String FILED_NAME = "상품 가격";

    public ProductPrice {
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

    public BigDecimal multiply(Quantity quantity) {
        return this.value.multiply(BigDecimal.valueOf(quantity.value()));
    }
}
