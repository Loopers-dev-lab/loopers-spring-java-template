package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Order API", description = "주문 관련 API 입니다.")
public interface OrderApiSpec {

    @Operation(
            summary = "주문 요청",
            description = "상품 주문을 생성합니다."
    )
    ApiResponse<OrderDto.OrderResponse> createOrder(
            @Parameter(
                    name = "X-USER-ID",
                    in = ParameterIn.HEADER,
                    description = "요청자 사용자 ID 헤더"
            )
            String xUserId,
            @Schema(name = "주문 요청", description = "주문 요청 DTO")
            OrderDto.CreateOrderRequest request
    );

    @Operation(
            summary = "유저의 주문 목록 조회",
            description = "로그인 ID로 주문 목록을 조회합니다."
    )
    ApiResponse<List<OrderDto.OrderResponse>> getOrders(
            @Parameter(
                    name = "X-USER-ID",
                    in = ParameterIn.HEADER,
                    description = "요청자 사용자 ID 헤더"
            )
            String xUserId
    );

    @Operation(
            summary = "단일 주문 상세 조회",
            description = "주문 ID로 주문 상세 정보를 조회합니다."
    )
    ApiResponse<OrderDto.OrderResponse> getOrder(
            @Parameter(
                    name = "orderId",
                    in = ParameterIn.PATH,
                    description = "주문 ID"
            )
            Long orderId
    );
}

