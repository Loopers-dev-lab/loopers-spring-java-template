package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;

public class BrandV1Dto {
    public record BrandResponse(Long id, String name, String description) {
        public static BrandResponse from(BrandInfo brand) {
            return new BrandResponse(
                    brand.id(),
                    brand.name(),
                    brand.description()
            );
        }
    }
}
