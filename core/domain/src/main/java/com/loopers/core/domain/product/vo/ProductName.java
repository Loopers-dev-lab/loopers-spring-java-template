package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record ProductName(String value) {

    public ProductName {
        validateNotNull(value);
    }

    private static void validateNotNull(String value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("상품명"));
    }
}
