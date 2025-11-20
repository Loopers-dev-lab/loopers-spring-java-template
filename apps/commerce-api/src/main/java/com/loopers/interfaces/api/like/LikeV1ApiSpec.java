package com.loopers.interfaces.api.like;


import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Like V1 API", description = "Like API 입니다.")
public interface LikeV1ApiSpec {
    @Operation(summary = "좋아요 등록")
    ApiResponse<LikeV1Dto.LikeResponse> doLike (LikeV1Dto.LikeRequest request);

    @Operation(summary = "좋아요 취소")
    void doUnLike (Long userId, Long productId);

}
