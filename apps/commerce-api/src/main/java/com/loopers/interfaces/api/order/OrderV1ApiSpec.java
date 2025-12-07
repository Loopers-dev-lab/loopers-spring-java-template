package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Order V1 API", description = "Loopers 예시 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
            summary = "유저의 주문 목록 조회",
            description = "유저 ID로 주문 목록을 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderListResponse> getOrders(
            @Schema(name = "유저 ID", description = "조회할 유저의 ID")
            Long userId
    );

    @Operation(
            summary = "단일 주문 상세 조회",
            description = "유저의 단일 주문 상세를 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderDetailResponse> getOrder(
            @Schema(name = "유저 ID", description = "조회할 유저의 ID")
            Long userId,
            Long orderId
    );

    @Operation(
            summary = "주문 요청",
            description = "주문 상품 리스트를 전달받아 주문을 저장합니다."
    )
    ApiResponse<OrderV1Dto.OrderCreateResponse> createOrder(
            @Schema(name = "유저 ID", description = "조회할 유저의 ID")
            Long userId,
            @Schema(name = "주문 요청", description = "주문 상품 리스트와 결제방식")
            OrderV1Dto.OrderCreateRequest request
    );
}
