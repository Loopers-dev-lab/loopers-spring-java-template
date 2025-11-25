package com.loopers.application.api.brand;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.loopers.application.api.brand.BrandV1Dto.GetBrandResponse;

@Tag(name = "Brand V1 API", description = "브랜드 API 입니다.")
public interface BrandV1ApiSpec {

    @Operation(
            summary = "브랜드 조회",
            description = "ID로 브랜드를 조회합니다."
    )
    ApiResponse<GetBrandResponse> getBrand(String brandId);
}
