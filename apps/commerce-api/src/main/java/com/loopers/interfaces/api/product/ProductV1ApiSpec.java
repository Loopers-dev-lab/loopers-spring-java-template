package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Product V1 API", description = "상품 관리 API")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 생성",
        description = "새로운 상품을 생성합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> createProduct(
        ProductV1Dto.CreateProductRequest request
    );

    @Operation(
        summary = "상품 조회",
        description = "상품 ID로 상품 상세 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @Schema(description = "상품 ID")
        Long productId
    );

    @Operation(
        summary = "전체 상품 목록 조회",
        description = "전체 상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<Page<ProductV1Dto.ProductResponse>> getProducts(
        Pageable pageable
    );

    @Operation(
        summary = "브랜드별 상품 목록 조회",
        description = "특정 브랜드의 상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<Page<ProductV1Dto.ProductResponse>> getProductsByBrand(
        @Schema(description = "브랜드 ID")
        Long brandId,
        Pageable pageable
    );

    @Operation(
        summary = "상품 정보 수정",
        description = "상품 정보를 수정합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> updateProduct(
        @Schema(description = "상품 ID")
        Long productId,
        ProductV1Dto.UpdateProductRequest request
    );
}
