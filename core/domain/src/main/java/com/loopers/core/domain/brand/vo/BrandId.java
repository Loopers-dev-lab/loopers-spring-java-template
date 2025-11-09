package com.loopers.core.domain.brand.vo;

public record BrandId(String value) {

    public static BrandId empty() {
        return new BrandId(null);
    }
}
