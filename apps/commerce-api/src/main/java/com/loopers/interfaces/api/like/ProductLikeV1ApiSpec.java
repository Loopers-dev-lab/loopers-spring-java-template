package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product Like V1 API", description = "상품 좋아요 API")
public interface ProductLikeV1ApiSpec {

    @Operation(
            summary = "상품 좋아요 추가",
            description = "지정된 상품에 좋아요를 추가합니다. 이미 좋아요가 있는 경우 멱등성을 보장합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    ApiResponse<ProductLikeV1Dto.LikeResponse> addProductLike(
            @Parameter(description = "상품 ID", required = true) Long productId,
            @Parameter(description = "사용자 ID (헤더에서 추출)", hidden = true) Long userId
    );

    @Operation(
            summary = "상품 좋아요 취소",
            description = "지정된 상품의 좋아요를 취소합니다. 좋아요가 없는 경우 멱등성을 보장합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    ApiResponse<ProductLikeV1Dto.LikeResponse> removeProductLike(
            @Parameter(description = "상품 ID", required = true) Long productId,
            @Parameter(description = "사용자 ID (헤더에서 추출)", hidden = true) Long userId
    );

    @Operation(
            summary = "내가 좋아요한 상품 목록 조회",
            description = "사용자가 좋아요한 상품 목록을 최신 순으로 페이징하여 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<ProductLikeV1Dto.LikedProductsResponse> getLikedProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") int size,
            @Parameter(description = "사용자 ID (헤더에서 추출)", hidden = true) Long userId
    );
}