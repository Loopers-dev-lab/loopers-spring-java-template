package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import jakarta.validation.constraints.NotBlank;

public class BrandV1Dto {

    public record CreateBrandRequest(
        @NotBlank(message = "브랜드명은 필수입니다.")
        String name,
        String description
    ) {
    }

    public record UpdateBrandRequest(
        String name,
        String description
    ) {
    }

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
