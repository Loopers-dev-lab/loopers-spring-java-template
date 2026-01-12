package com.loopers.interfaces.api.order;

import com.loopers.domain.user.UserId;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Order V1 API", description = "Loopers 주문 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "여러 상품을 주문하고 결제합니다. 재고 차감 및 포인트 차감이 자동으로 처리됩니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @Parameter(name = "X-USER-ID", description = "주문할 유저의 ID", required = true)
        UserId userId,
        @Schema(name = "주문 요청", description = "주문할 상품 목록")
        OrderV1Dto.CreateOrderRequest request
    );

    @Operation(
        summary = "주문 조회",
        description = "주문 ID로 주문을 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @Parameter(name = "id", description = "조회할 주문의 ID", required = true)
        Long id
    );

    @Operation(
        summary = "유저 주문 목록 조회",
        description = "유저의 주문 목록을 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrdersResponse> getUserOrders(
        @Parameter(name = "X-USER-ID", description = "조회할 유저의 ID", required = true)
        UserId userId
    );
}

