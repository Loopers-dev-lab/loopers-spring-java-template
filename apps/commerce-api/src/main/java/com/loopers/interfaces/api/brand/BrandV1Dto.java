package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;

public class BrandV1Dto {

    public record BrandResponse(
            Long id,
            String name,
            String description
    ) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(
                    info.id(),
                    info.name(),
                    info.description()
            );
        }
    }
}

