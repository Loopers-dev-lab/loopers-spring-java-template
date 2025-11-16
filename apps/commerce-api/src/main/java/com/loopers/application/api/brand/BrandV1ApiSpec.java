package com.loopers.application.api.brand;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.loopers.application.api.brand.BrandV1Dto.GetBrandResponse;

@Tag(name = "Brand V1 API", description = "Brand API 입니다.")
public interface BrandV1ApiSpec {

    @Operation(
            summary = "사용자 조회",
            description = "ID로 사용자를 조회합니다."
    )
    ApiResponse<GetBrandResponse> getBrand(String brandId);

}
