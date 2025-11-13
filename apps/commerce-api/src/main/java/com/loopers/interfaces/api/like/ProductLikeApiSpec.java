package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product Like API", description = "상품 좋아요 관련 API 입니다.")
public interface ProductLikeApiSpec {

    @Operation(
            summary = "상품 좋아요 등록",
            description = "로그인한 사용자가 상품에 좋아요를 등록합니다. 멱등성을 보장합니다."
    )
    ApiResponse<ProductLikeDto.LikeResponse> likeProduct(
            @Parameter(description = "사용자 ID (헤더)", required = true)
            String userId,

            @Parameter(description = "상품 ID", required = true)
            Long productId
    );

    @Operation(
            summary = "상품 좋아요 취소",
            description = "로그인한 사용자가 상품 좋아요를 취소합니다. 멱등성을 보장합니다."
    )
    ApiResponse<ProductLikeDto.LikeResponse> unlikeProduct(
            @Parameter(description = "사용자 ID (헤더)", required = true)
            String userId,

            @Parameter(description = "상품 ID", required = true)
            Long productId
    );
}
