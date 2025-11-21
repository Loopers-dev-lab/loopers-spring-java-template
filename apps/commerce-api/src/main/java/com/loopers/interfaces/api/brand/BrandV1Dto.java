package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.Brand;

public class BrandV1Dto {
    public record BrandResponse(Long id, String name) {
        public static BrandResponse from(Brand brand) {
            return new BrandResponse(
                brand.getId(),
                brand.getName()
            );
        }
    }
}

