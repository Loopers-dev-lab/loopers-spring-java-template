package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.BrandStatus;
import lombok.Builder;

public class BrandDto {

    @Builder
    public record BrandResponse(
            Long id,
            String name,
            String description,
            BrandStatus status,
            Boolean isVisible,
            Boolean isSellable
    ) {
        public static BrandResponse from(BrandInfo brandInfo) {
            return BrandResponse.builder()
                    .id(brandInfo.id())
                    .name(brandInfo.name())
                    .description(brandInfo.description())
                    .status(brandInfo.status())
                    .isVisible(brandInfo.isVisible())
                    .isSellable(brandInfo.isSellable())
                    .build();
        }
    }
}

