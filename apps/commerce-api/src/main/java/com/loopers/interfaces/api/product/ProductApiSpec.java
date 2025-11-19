package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product API", description = "상품 관련 API 입니다.")
public interface ProductApiSpec {

    @Operation(summary = "상품 목록 조회", description = "상품 목록 조회")
    ApiResponse<ProductDto.ProductListResponse> getProducts(@Parameter(description = "브랜드 ID (선택)") Long brandId,

                                                            @Parameter(description = "정렬 기준", schema = @Schema(allowableValues = {"latest", "price_asc", "likes_desc"}, defaultValue = "latest")) String sort,

                                                            @Parameter(description = "페이지 번호", schema = @Schema(defaultValue = "0")) int page,

                                                            @Parameter(description = "페이지당 상품 수", schema = @Schema(defaultValue = "20")) int size);

    @Operation(
            summary = "상품 상세 조회",
            description = "상품 ID로 상세 정보를 조회합니다."
    )
    ApiResponse<ProductDto.ProductDetailResponse> getProduct(
            @Parameter(description = "상품 ID", required = true)
            Long productId
    );
}
