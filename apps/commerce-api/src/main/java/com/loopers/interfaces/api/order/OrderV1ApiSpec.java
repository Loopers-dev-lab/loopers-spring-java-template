package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Order V1 API", description = "주문 관리 API")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "새로운 주문을 생성합니다. 쿠폰 적용, 재고 차감, 포인트 차감이 원자적으로 처리됩니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @Schema(description = "사용자 ID")
        String userId,
        OrderV1Dto.CreateOrderRequest request
    );

    @Operation(
        summary = "주문 조회",
        description = "주문 ID로 주문 상세 정보를 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @Schema(description = "주문 ID")
        Long orderId
    );

    @Operation(
        summary = "사용자 주문 목록 조회",
        description = "특정 사용자의 주문 목록을 페이징하여 조회합니다."
    )
    ApiResponse<Page<OrderV1Dto.OrderResponse>> getOrdersByUser(
        @Schema(description = "사용자 ID")
        String userId,
        Pageable pageable
    );

    @Operation(
        summary = "주문 취소",
        description = "주문을 취소합니다. 재고와 포인트가 복구됩니다."
    )
    ApiResponse<Void> cancelOrder(
        @Schema(description = "주문 ID")
        Long orderId,
        @Schema(description = "사용자 ID")
        String userId
    );
}
