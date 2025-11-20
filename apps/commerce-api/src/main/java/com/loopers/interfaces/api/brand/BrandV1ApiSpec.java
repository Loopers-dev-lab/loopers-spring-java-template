package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Brand V1 API", description = "브랜드 관리 API")
public interface BrandV1ApiSpec {

    @Operation(
        summary = "브랜드 생성",
        description = "새로운 브랜드를 생성합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> createBrand(
        BrandV1Dto.CreateBrandRequest request
    );

    @Operation(
        summary = "브랜드 조회 (ID)",
        description = "브랜드 ID로 브랜드 정보를 조회합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> getBrand(
        @Schema(description = "브랜드 ID")
        Long brandId
    );

    @Operation(
        summary = "브랜드 조회 (이름)",
        description = "브랜드 이름으로 브랜드 정보를 조회합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> getBrandByName(
        @Schema(description = "브랜드 이름")
        String name
    );

    @Operation(
        summary = "브랜드 정보 수정",
        description = "브랜드 정보를 수정합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> updateBrand(
        @Schema(description = "브랜드 ID")
        Long brandId,
        BrandV1Dto.UpdateBrandRequest request
    );
}
