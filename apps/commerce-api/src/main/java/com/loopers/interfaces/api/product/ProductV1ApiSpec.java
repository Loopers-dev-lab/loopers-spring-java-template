package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Product V1 API", description = "상품 API 입니다.")
public interface ProductV1ApiSpec {
    // POST /api/v1/products
    @Operation(
            method = "POST",
            summary = "상품 등록",
            description = "새로운 상품을 등록합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> createProduct(
            @Schema(
                    name = "상품 등록 요청",
                    description = "등록할 상품의 정보를 포함한 요청 객체"
            )
            ProductV1Dto.ProductCreateRequest request
    );


    // /api/v1/products - GET
    @Operation(
            method = "GET",
            summary = "상품 목록 조회",
            description = "상품 목록을 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductsPageResponse> getProductList(
            @Schema(
                    name = "페이지 정보",
                    description = "페이지 번호, 페이지 크기, 정렬 정보를 포함한 페이지 정보" +
                            "\n- sort 옵션: latest (최신순), price_asc (가격 오름차순), like_desc (좋아요 내림차순)" +
                            "\n- 기본값: page=0, size=20, sort=latest"
            )
            Pageable pageable
    );

    // /api/v1/products/{productId} - GET
    @Operation(
            method = "GET",
            summary = "상품 상세 조회",
            description = "상품 상세 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProductDetail(
            @Schema(
                    name = "상품 ID",
                    description = "조회할 상품의 ID"
            )
            Long productId
    );
}
