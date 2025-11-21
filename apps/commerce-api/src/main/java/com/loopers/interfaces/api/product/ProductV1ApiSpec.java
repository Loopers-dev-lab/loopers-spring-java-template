package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 상세 조회",
            description = "상품에 대한 상세조회를 한다."
    )
    ApiResponse<ProductV1DTO.ProductDetailResponse> getProductDetail(
            @Schema(name = "상품 상세 조회", description = "상품 상세 조회에 필요한 정보")
            Long productId
    );
}
