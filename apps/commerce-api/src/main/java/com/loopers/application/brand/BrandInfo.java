package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import lombok.Builder;

@Builder
public record BrandInfo(
        Long id,
        String name,
        String description,
        BrandStatus status,
        Boolean isVisible,
        Boolean isSellable
) {
    public static BrandInfo from(Brand brand) {
        return BrandInfo.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .isVisible(brand.getIsVisible())
                .isSellable(brand.getIsSellable())
                .build();
    }
}

