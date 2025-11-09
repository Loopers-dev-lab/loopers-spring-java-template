package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record ProductStock(Long value) {

    public static ProductStock create(Long value) {
        validateNotNull(value);
        validateNotNegative(value);

        return new ProductStock(value);
    }

    private static void validateNotNull(Long value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("상품 재고"));
    }

    private static void validateNotNegative(Long value) {
        if (value < 0) {
            throw new IllegalArgumentException(DomainErrorCode.COULD_NOT_BE_PRODUCT_PRICE_NEGATIVE.getMessage());
        }
    }
}
