package com.loopers.interfaces.api.like.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Like Product V1 API", description = "상품 좋아요 API 입니다.")
public interface LikeProductV1ApiSpec {
    // /api/v1/like/products/{productId} - POST
    @Operation(
            method = "POST",
            summary = "상품 좋아요 추가",
            description = "회원이 특정 상품에 좋아요를 추가합니다."
    )
    ApiResponse<Void> likeProduct(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            Long productId
    );

    // /api/v1/like/products/{productId} - DELETE
    @Operation(
            method = "DELETE",
            summary = "상품 좋아요 취소",
            description = "회원이 특정 상품에 대한 좋아요를 취소합니다."
    )
    ApiResponse<Void> unlikeProduct(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            Long productId
    );

    // /api/v1/like/products - GET
    @Operation(
            method = "GET",
            summary = "회원이 좋아요한 상품 목록 조회",
            description = "회원이 좋아요한 상품들의 목록을 조회합니다."
    )
    ApiResponse<LikeProductV1Dto.ProductsResponse> getLikedProducts(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Schema(
                    name = "페이지 정보",
                    description = "페이지 번호, 페이지 크기, 정렬 정보를 포함한 페이지 정보" +
                            "\n- 기본값: page=0, size=20"
            )
            Pageable pageable
    );
}
