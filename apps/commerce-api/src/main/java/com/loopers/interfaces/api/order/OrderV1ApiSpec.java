package com.loopers.interfaces.api.order;


import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Order V1 API", description = "Order V1 API Spec")
public interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성")
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(@RequestBody OrderV1Dto.OrderRequest request);

    @Operation(summary = "주문 조회")
    ApiResponse<OrderV1Dto.OrderResponse> findOrderById(Long id);
}
