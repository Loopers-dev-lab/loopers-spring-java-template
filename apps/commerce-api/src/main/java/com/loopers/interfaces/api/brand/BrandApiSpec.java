package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Brand API", description = "브랜드 관련 API 입니다.")
public interface BrandApiSpec {

    @Operation(
            summary = "브랜드 상세 조회",
            description = "브랜드 ID로 브랜드 정보와 해당 브랜드의 상품 목록을 조회합니다."
    )
    ApiResponse<BrandDto.BrandDetailResponse> getBrand(
            @Parameter(description = "브랜드 ID", required = true)
            Long brandId
    );
}
