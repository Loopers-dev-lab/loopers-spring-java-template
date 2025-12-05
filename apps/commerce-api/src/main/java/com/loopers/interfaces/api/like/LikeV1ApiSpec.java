package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Like API", description = "상품 좋아요 관리 API")
public interface LikeV1ApiSpec {

    @Operation(summary = "상품 좋아요 등록", description = "특정 상품에 좋아요를 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "좋아요 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "상품을 찾을 수 없음"
            )
    })
    @PostMapping("/products/{productId}")
    ApiResponse<Void> addLike(
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @Parameter(description = "상품 ID", required = true)
            @PathVariable("productId") Long productId
    );

    @Operation(summary = "상품 좋아요 취소", description = "특정 상품의 좋아요를 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "좋아요 취소 성공"
            )
    })
    @DeleteMapping("/products/{productId}")
    ApiResponse<Void> removeLike(
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @Parameter(description = "상품 ID", required = true)
            @PathVariable("productId") Long productId
    );
}
