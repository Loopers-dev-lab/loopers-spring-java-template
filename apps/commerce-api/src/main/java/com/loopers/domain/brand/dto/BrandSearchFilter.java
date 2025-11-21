package com.loopers.domain.brand.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrandSearchFilter {
    private String brandName;

    public static BrandSearchFilter of(String brandName) {
        return BrandSearchFilter.builder()
                .brandName(brandName)
                .build();
    }
}
