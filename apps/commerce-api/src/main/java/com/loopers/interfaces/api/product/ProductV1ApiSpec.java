package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product V1 API", description = "Product API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 조회",
            description = "상품을 조회 합니다. 성공기 상품 list를 반환합니다."
    )
    ApiResponse<ProductV1Dto.ListResponse> getProducts(
            Long brandId,
            String sort,
            int page,
            int size
    );
}

