package com.loopers.application.api.order;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Order V1 API", description = "주문 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
            summary = "주문 요청",
            description = "상품 목록으로 주문을 요청합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> order(
            String userIdentifier,
            OrderV1Dto.OrderRequest request
    );
}
