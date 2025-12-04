package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;

public class BrandV1Dto {
    public record BrandResponse(
            Long id,
            String brandName,
            boolean isActive
    ) {
        public static BrandResponse from(BrandInfo brandInfo) {
            return new BrandResponse(
                    brandInfo.id(),
                    brandInfo.brandName(),
                    brandInfo.isActive()
            );
        }
    }
}
