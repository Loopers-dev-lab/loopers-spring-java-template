package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product V1 API", description = "Product V1 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 목록 조회",
            description = "브랜드 ID, 정렬 기준, 페이지 정보를 이용하여 상품 목록을 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductListResponse> getProducts(
            @Parameter(name = "brandId", description = "브랜드 ID (선택사항)", required = false)
            Long brandId,
            @Parameter(name = "sort", description = "정렬 기준 (latest, price_asc, likes_desc)", required = false)
            String sort,
            @Parameter(name = "page", description = "페이지 번호 (기본값: 0)", required = false)
            Integer page,
            @Parameter(name = "size", description = "페이지당 상품 수 (기본값: 20)", required = false)
            Integer size
    );

    @Operation(
            summary = "상품 정보 조회",
            description = "상품 ID로 상품 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(
            @Schema(name = "상품 ID", description = "조회할 상품의 ID")
            Long productId
    );
}
