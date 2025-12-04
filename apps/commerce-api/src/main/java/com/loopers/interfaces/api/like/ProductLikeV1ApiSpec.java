package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

public interface ProductLikeV1ApiSpec {

    @Operation(
            summary = "상품 좋아요 추가",
            description = "상품에 대한 좋아요 추가를 처리한다"
    )
    ApiResponse<ProductLikeV1Dto.ProductLikeResponse> addProductLike(
            @Schema(name = "상품 좋아요 추가", description = "상품 좋아요 추가시 필요한 상품 정보")
            Long productId,
            @Parameter(
                    name = "X-USER-ID",
                    description = "요청 헤더로 전달되는 회원 ID",
                    in = ParameterIn.HEADER,
                    required = true
            )
            String headerUserId
    );

}
