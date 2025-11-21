package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Like V1", description = "좋아요 관리 API")
public interface LikeV1ApiSpec {

    @Operation(summary = "상품 좋아요", description = "특정 상품에 좋아요를 추가합니다.")
    ApiResponse<Void> likeProduct(
            @PathVariable Long productId,
            @RequestHeader("X-USER-ID") String memberId
    );

    @Operation(summary = "상품 좋아요 취소", description = "특정 상품의 좋아요를 취소합니다.")
    ApiResponse<Void> unlikeProduct(
            @PathVariable Long productId,
            @RequestHeader("X-USER-ID") String memberId
    );
}