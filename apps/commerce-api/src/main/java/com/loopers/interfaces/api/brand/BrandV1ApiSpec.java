package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Brand V1 API", description = "Loopers 브랜드 API 입니다.")
public interface BrandV1ApiSpec {

    @Operation(
        summary = "브랜드 정보 조회",
        description = "브랜드 이름으로 브랜드 정보를 조회합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> getBrand(
        @Parameter(name = "brandId", description = "조회할 브랜드의 이름 (brandId는 브랜드 이름을 의미)", required = true)
        String brandId
    );
}

