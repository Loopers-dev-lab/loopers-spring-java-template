package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;

@Tag(name = "Product API", description = "상품 관련 API 입니다.")
public interface ProductApiSpec {

    @Operation(
            summary = "상품 목록 조회"
            , description = "상품 목록을 페이징하여 조회합니다. 검색 조건과 정렬 옵션을 지원합니다."
    )
    ApiResponse<Page<ProductDto.ProductResponse>> getProducts(
            @Schema(name = "검색 조건", description = "상품 검색 조건 및 페이징 정보")
            ProductDto.SearchRequest request
    );

    @Operation(
            summary = "상품 상세 조회"
            , description = "상품 ID로 상품 상세 정보를 조회합니다."
    )
    ApiResponse<ProductDto.ProductResponse> getProduct(
            @Parameter(
                    name = "productId"
                    , in = ParameterIn.PATH
                    , description = "상품 ID"
                    , required = true
            )
            Long productId
    );
}

