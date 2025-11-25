package com.loopers.interfaces.api.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;

@Tag(name = "Product V1 API", description = "상품 관리 API")
public interface ProductV1ApiSpec {

    @Operation(
            summary = "상품 목록 조회",
            description = "상품 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>> getProducts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String productName
    );

    @Operation(
            summary = "상품 상세 조회",
            description = "상품 ID로 상품 상세 정보를 조회합니다. 로그인한 사용자의 경우 좋아요 여부도 함께 조회됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    ApiResponse<ProductV1Dtos.ProductDetailResponse> getProductDetail(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable
            Long productId,

            @Parameter(description = "사용자명 (선택)", example = "testuser")
            @RequestHeader(value = "X-USER-ID", required = false)
            String username
    );
}

