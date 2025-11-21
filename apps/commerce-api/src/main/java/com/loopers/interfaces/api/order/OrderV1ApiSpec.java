package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestBody;

public interface OrderV1ApiSpec {
    @Operation(
            summary = "주문 생성",
            description = "새로운 주문을 등록한다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(
            @RequestBody
            @Schema(name = "주문 생성", description = "주문 생성시 필요한 주문 정보")
            OrderV1Dto.OrderRequest request
    );
}
