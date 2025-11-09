package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.math.BigDecimal;
import java.util.Objects;

public record ProductPrice(BigDecimal value) {

    public static ProductPrice create(BigDecimal value) {
        validateNotNull(value);
        validateNotNegative(value);

        return new ProductPrice(value);
    }

    private static void validateNotNull(BigDecimal value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("상품 가격"));
    }

    private static void validateNotNegative(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(DomainErrorCode.COULD_NOT_BE_PRODUCT_PRICE_NEGATIVE.getMessage());
        }
    }
}
