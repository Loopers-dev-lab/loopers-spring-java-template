package com.loopers.interfaces.api.like;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.loopers.interfaces.api.ApiResponse;

@Tag(name = "Like V1 API", description = "좋아요 관리 API")
public interface LikeV1ApiSpec {

    /**
     * Register a like for the specified product by the given user.
     *
     * If the user has already liked the product, the existing like is preserved and the call has no additional effect.
     *
     * @param username  the user who performs the like
     * @param productId the ID of the product to like
     * @return an ApiResponse containing the created or existing LikeResponse with like details
     */
    @Operation(
            summary = "좋아요 등록",
            description = "상품에 좋아요를 등록합니다. 이미 좋아요한 상품이면 무시됩니다."
    )
    ApiResponse<LikeV1Dtos.LikeResponse> upsertLike(
            @Schema(name = "사용자명", description = "좋아요할 사용자명")
            String username,

            @Schema(name = "상품 ID", description = "좋아요할 상품의 ID")
            Long productId
    );

    /**
     * Cancels a user's like for the specified product.
     *
     * @param username the username whose like will be removed
     * @param productId the ID of the product to unlike
     * @return an ApiResponse with no content
     */
    @Operation(
            summary = "좋아요 취소",
            description = "상품의 좋아요를 취소합니다."
    )
    ApiResponse<Void> unlikeProduct(
            @Schema(name = "사용자명", description = "좋아요 취소할 사용자명")
            String username,

            @Schema(name = "상품 ID", description = "좋아요 취소할 상품의 ID")
            Long productId
    );
}
