package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "주문 V1 API", description = "유저의 주문을 처리하는 API")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 전 선조회",
        description = "OrderV1Dto.OrderRequest 포맷으로 선조회를 처리합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> preOrder(
            @Schema(name="", description = "주문 전 재고 및 결제 가능여부 확인")
            OrderV1Dto.OrderRequest request
    );


    @Operation(
            summary = "주문 실행",
            description = "OrderV1Dto.OrderRequest 포맷으로 주문를 처리합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @Schema(name="", description = "주문 실행, 재고 및 결제 가능여부 확인 후 주문 처리")
            OrderV1Dto.OrderRequest request
    );
}
