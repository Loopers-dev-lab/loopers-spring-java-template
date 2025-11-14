package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Product V1 API", description = "Product API 입니다.")
public interface ProductV1ApiSpec {
    @Operation(summary = "상품 등록")
    ApiResponse<ProductV1Dto.ProductResponse> registerProduct (ProductV1Dto.ProductRequest request);

    @Operation(summary = "상품 목록 조회")
    ApiResponse<List<ProductV1Dto.ProductResponse>> findAllProducts ();

    @Operation(summary = "상품 상세 조회")
    ApiResponse<ProductV1Dto.ProductResponse> findProductById (Long id);

}
