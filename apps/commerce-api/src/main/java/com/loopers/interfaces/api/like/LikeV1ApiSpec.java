package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Like V1 API", description = "상품 좋아요 관리 API")
public interface LikeV1ApiSpec {

    @Operation(
        summary = "좋아요 추가",
        description = "상품에 좋아요를 추가합니다. 이미 좋아요한 경우 무시됩니다(멱등성)."
    )
    ApiResponse<Void> like(
        @Schema(description = "사용자 ID")
        String userId,
        @Schema(description = "상품 ID")
        Long productId
    );

    @Operation(
        summary = "좋아요 취소",
        description = "상품의 좋아요를 취소합니다. 좋아요하지 않은 경우에도 정상 처리됩니다(멱등성)."
    )
    ApiResponse<Void> unlike(
        @Schema(description = "사용자 ID")
        String userId,
        @Schema(description = "상품 ID")
        Long productId
    );

    @Operation(
        summary = "사용자 좋아요 목록 조회",
        description = "특정 사용자가 좋아요한 상품 목록을 페이징하여 조회합니다."
    )
    ApiResponse<Page<LikeV1Dto.LikeResponse>> getLikesByUser(
        @Schema(description = "사용자 ID")
        String userId,
        Pageable pageable
    );

    @Operation(
        summary = "상품 좋아요 수 조회",
        description = "특정 상품의 좋아요 수를 조회합니다."
    )
    ApiResponse<LikeV1Dto.LikeCountResponse> getLikeCount(
        @Schema(description = "상품 ID")
        Long productId
    );

    @Operation(
        summary = "좋아요 여부 확인",
        description = "사용자가 특정 상품을 좋아요했는지 확인합니다."
    )
    ApiResponse<LikeV1Dto.IsLikedResponse> isLiked(
        @Schema(description = "사용자 ID")
        String userId,
        @Schema(description = "상품 ID")
        Long productId
    );
}
