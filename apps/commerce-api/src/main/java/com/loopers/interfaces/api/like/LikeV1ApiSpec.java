package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "상품 좋아요 V1 API", description = "유저의 상품 좋아요 API")
public interface LikeV1ApiSpec {
    @Operation(
            summary = "상품 좋아요",
            description = "OrderV1Dto.OrderRequest 포맷으로 선조회를 처리합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> productLike(
            @Schema(name="", description = "상품 좋아요")
            OrderV1Dto.OrderRequest request
    );


    @Operation(
            summary = "상품 좋아요 취소",
            description = "OrderV1Dto.OrderRequest 포맷으로 주문를 처리합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> productDisLike(
            @Schema(name="", description = "상품 좋아요 취소")
            OrderV1Dto.OrderRequest request
    );
}
