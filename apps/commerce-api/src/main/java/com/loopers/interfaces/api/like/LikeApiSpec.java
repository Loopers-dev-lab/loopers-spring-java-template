package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Like API", description = "좋아요 관련 API 입니다.")
public interface LikeApiSpec {

    @Operation(
            summary = "상품 좋아요 등록"
            , description = "상품에 좋아요를 등록합니다."
    )
    ApiResponse<Object> saveProductLike(
            @Parameter(
                    name = "X-USER-ID"
                    , in = ParameterIn.HEADER
                    , description = "요청자 사용자 ID 헤더"
            )
            String xUserId,
            @Parameter(
                    name = "productId"
                    , in = ParameterIn.PATH
                    , description = "상품 ID"
            )
            Long productId
    );

    @Operation(
            summary = "상품 좋아요 취소"
            , description = "상품 좋아요를 취소합니다."
    )
    ApiResponse<Object> deleteProductLike(
            @Parameter(
                    name = "X-USER-ID"
                    , in = ParameterIn.HEADER
                    , description = "요청자 사용자 ID 헤더"
            )
            String xUserId,
            @Parameter(
                    name = "productId"
                    , in = ParameterIn.PATH
                    , description = "상품 ID"
            )
            Long productId
    );
}

