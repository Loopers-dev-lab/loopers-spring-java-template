package com.loopers.application.api.product;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product V1 API", description = "상품 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 정보 조회",
            description = "상품 정보를 조회합니다."
    )
    ApiResponse<ProductV1Dto.GetProductResponse> getProduct(String productId);

    @Operation(
            summary = "상품 목록 조회",
            description = "상품 목록을 조회합니다."
    )
    ApiResponse<ProductV1Dto.GetProductListResponse> getProductList(
            String brandId,
            String createdAtSort,
            String priceSort,
            String likeCountSort,
            int pageNo,
            int pageSize
    );

}
