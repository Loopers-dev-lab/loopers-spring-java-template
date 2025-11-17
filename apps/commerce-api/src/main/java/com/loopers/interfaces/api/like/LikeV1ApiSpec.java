package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Like V1 API", description = "Like V1 API 입니다.")
public interface LikeV1ApiSpec {

    @Operation(
            summary = "상품 좋아요 등록",
            description = "상품 ID로 상품에 좋아요를 등록합니다."
    )
    ApiResponse<LikeV1Dto.LikeResponse> likeProduct(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            String loginId,
            @Parameter(name = "productId", description = "상품 ID", required = true)
            Long productId
    );

    @Operation(
            summary = "상품 좋아요 취소",
            description = "상품 ID로 상품의 좋아요를 취소합니다."
    )
    ApiResponse<LikeV1Dto.LikeResponse> cancelLikeProduct(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            String loginId,
            @Parameter(name = "productId", description = "상품 ID", required = true)
            Long productId
    );

    @Operation(
            summary = "내가 좋아요 한 상품 목록 조회",
            description = "로그인 ID로 내가 좋아요 한 상품 목록을 조회합니다."
    )
    ApiResponse<LikeV1Dto.LikedProductListResponse> getLikedProducts(
            @Parameter(name = "X-USER-ID", description = "로그인 ID", required = true)
            String loginId
    );
}

