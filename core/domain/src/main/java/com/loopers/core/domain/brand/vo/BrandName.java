package com.loopers.core.domain.brand.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record BrandName(String value) {

    public static BrandName create(String value) {
        validateNotNull(value);

        return new BrandName(value);
    }

    private static void validateNotNull(String value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("브랜드명"));
    }

}
