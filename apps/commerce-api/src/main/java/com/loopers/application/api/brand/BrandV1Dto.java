package com.loopers.application.api.brand;

import com.loopers.core.domain.brand.Brand;

public class BrandV1Dto {

    public record GetBrandResponse(
            String name,
            String description
    ) {
        public static GetBrandResponse from(Brand brand) {
            return new GetBrandResponse(
                    brand.getName().value(),
                    brand.getDescription().value()
            );
        }
    }
}
