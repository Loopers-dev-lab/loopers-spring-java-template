package com.loopers.application.brand;

import com.loopers.domain.product.Brand;

public record BrandInfo(String name) {
    public static BrandInfo from(Brand brand) {
        return new BrandInfo(brand.name());
    }
}

