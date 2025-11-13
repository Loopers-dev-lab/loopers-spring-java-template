package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Order API", description = "주문 관련 API 입니다.")
public interface OrderApiSpec {

    @Operation(
            summary = "주문 생성",
            description = "여러 상품을 한 번에 주문하고 포인트로 결제합니다."
    )
    ApiResponse<OrderDto.OrderResponse> createOrder(
            @Parameter(description = "사용자 ID (헤더)", required = true)
            String userId,
            OrderDto.OrderCreateRequest request
    );

    @Operation(
            summary = "주문 목록 조회",
            description = "사용자의 주문 목록을 최신순으로 조회합니다."
    )
    ApiResponse<OrderDto.OrderListResponse> getOrders(
            @Parameter(description = "사용자 ID (헤더)", required = true)
            String userId
    );
}
