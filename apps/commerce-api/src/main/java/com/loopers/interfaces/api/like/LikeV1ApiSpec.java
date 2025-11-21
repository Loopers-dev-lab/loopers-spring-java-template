package com.loopers.interfaces.api.like;

import com.loopers.domain.user.UserId;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Like V1 API", description = "Loopers 좋아요 API 입니다.")
public interface LikeV1ApiSpec {

    @Operation(
        summary = "상품 좋아요 등록",
        description = "상품에 좋아요를 등록합니다. 멱등하게 동작하여 이미 좋아요가 있으면 아무것도 하지 않습니다."
    )
    ApiResponse<Void> addLike(
        @Parameter(name = "X-USER-ID", description = "좋아요를 등록할 유저의 ID", required = true)
        UserId userId,
        @Parameter(name = "productId", description = "좋아요를 등록할 상품의 ID", required = true)
        Long productId
    );

    @Operation(
        summary = "상품 좋아요 취소",
        description = "상품의 좋아요를 취소합니다. 멱등하게 동작하여 이미 좋아요가 없으면 아무것도 하지 않습니다."
    )
    ApiResponse<Void> removeLike(
        @Parameter(name = "X-USER-ID", description = "좋아요를 취소할 유저의 ID", required = true)
        UserId userId,
        @Parameter(name = "productId", description = "좋아요를 취소할 상품의 ID", required = true)
        Long productId
    );

    @Operation(
        summary = "내가 좋아요 한 상품 목록 조회",
        description = "유저가 좋아요한 상품 목록을 조회합니다."
    )
    ApiResponse<LikeV1Dto.LikedProductsResponse> getLikedProducts(
        @Parameter(name = "X-USER-ID", description = "조회할 유저의 ID", required = true)
        UserId userId
    );
}

