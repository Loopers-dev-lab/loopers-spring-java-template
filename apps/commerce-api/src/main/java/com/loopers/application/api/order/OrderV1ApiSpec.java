package com.loopers.application.api.order;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.loopers.application.api.order.OrderV1Dto.*;

@Tag(name = "Order V1 API", description = "주문 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
            summary = "주문 요청",
            description = "상품 목록으로 주문을 요청합니다."
    )
    ApiResponse<OrderResponse> order(String userIdentifier, OrderRequest request);

    @Operation(
            summary = "주문 목록 조회",
            description = "주문의 목록을 조회합니다."
    )
    ApiResponse<OrderListResponse> getOrderList(
            String userIdentifier,
            String createdAtSort,
            int pageNo,
            int pageSize
    );

    @Operation(
            summary = "주문 상세 조회",
            description = "주문의 상세 정보를 조회합니다."
    )
    ApiResponse<OrderDetailResponse> getOrderDetail(String orderId);
}
