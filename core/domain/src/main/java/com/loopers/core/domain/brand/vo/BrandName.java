package com.loopers.core.domain.brand.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record BrandName(String value) {
    public BrandName {
        validateNotNull(value);
    }

    private static void validateNotNull(String value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("브랜드명"));
    }

}
