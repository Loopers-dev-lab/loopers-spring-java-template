package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Like V1 API", description = "Loopers 예시 API 입니다.")
public interface LikeV1ApiSpec {

    @Operation(
            summary = "상품 좋아요 등록",
            description = "Product ID로 좋아요를 등록합니다."
    )
    ApiResponse<Object> addLike(
            Long userId,
            @Schema(name = "Product ID", description = "좋아요할 상품 ID")
            Long productId
    );

    @Operation(
            summary = "상품 좋아요 취소",
            description = "Product ID로 좋아요를 취소합니다."
    )
    ApiResponse<Object> removeLike(
            Long userId,
            @Schema(name = "Product ID", description = "좋아요 취소할 상품 ID")
            Long productId
    );

    @Operation(
            summary = "좋아요 한 상품 목록 조회",
            description = "내가 좋아요 한 상품 목록을 조회합니다."
    )
    ApiResponse<List<LikeV1Dto.LikedProductResponse>> getLikedProducts(
            Long userId
    );

}
