package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record ProductName(String value) {

    public static ProductName create(String value) {
        validateNotNull(value);

        return new ProductName(value);
    }

    private static void validateNotNull(String value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("상품명"));
    }
}
