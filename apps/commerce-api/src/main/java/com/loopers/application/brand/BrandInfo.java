package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;

public record BrandInfo(
        Long id,
        String brandName,
        boolean isActive
) {
    public static BrandInfo from(Brand brand) {
        return new BrandInfo(
                brand.getId(),
                brand.getBrandName(),
                brand.isActive()
        );
    }
}
