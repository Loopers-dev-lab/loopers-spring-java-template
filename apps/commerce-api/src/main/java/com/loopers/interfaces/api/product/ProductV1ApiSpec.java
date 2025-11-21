package com.loopers.interfaces.api.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Pageable;

import com.loopers.interfaces.api.ApiResponse;

@Tag(name = "Product V1 API", description = "상품 관리 API")
public interface ProductV1ApiSpec {

    /**
     * Retrieve a paginated list of products, optionally filtered by brand ID or product name.
     *
     * @param pageableLong pagination and sorting information for the requested page
     * @param brandId      optional brand identifier to filter products by brand
     * @param productName  optional substring to filter products by name
     * @return             an ApiResponse wrapping a PageResponse of ProductListResponse objects for the requested page
     */
    @Operation(
            summary = "상품 목록 조회",
            description = "상품 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ApiResponse<ProductV1Dtos.PageResponse<ProductV1Dtos.ProductListResponse>> getProducts(
            Pageable pageableLong,
            Long brandId,
            String productName
    );

    /**
     * Retrieve detailed information for a product by its ID, including the requesting user's like status when a username is supplied.
     *
     * @param productId the ID of the product to retrieve
     * @param username optional username; when provided, the response includes whether this user has liked the product
     * @return an ApiResponse containing a ProductDetailResponse with product information and optional like status
     */
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
            Long productId,

            @Parameter(description = "사용자명 (선택)", example = "testuser")
            String username
    );
}
