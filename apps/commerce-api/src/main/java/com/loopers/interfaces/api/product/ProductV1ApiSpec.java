package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product V1 API", description = "상품 조회 API입니다.")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "조회 시 브랜드 필터링, 정렬(최신, 가격 오름차순, 좋아요 내림차순)이 가능합니다."
    )
    ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>> getProducts(
            @RequestParam(required = false) Long brandId,
            Pageable pageable,
            @RequestParam(required = false) String sort
    );


    @Operation(
            summary = "상품 정보 조회",
            description = "조회 시 총 좋아요 수도 함께 조회됩니다."
    )
    ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
            @Schema(name = "상품 ID", description = "조회할 상품의 ID")
            Long productId
    );
}
