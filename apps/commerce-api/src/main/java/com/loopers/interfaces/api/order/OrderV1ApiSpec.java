package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order API", description = "주문 및 결제 API")
public interface OrderV1ApiSpec {

    @Operation(summary = "주문 요청", description = "상품을 주문하고 결제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주문 성공",
                    content = @Content(schema = @Schema(implementation = OrderV1Dto.OrderResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "재고 부족, 잔액 부족, 카드 정보 누락 등",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "PG 시스템 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping
    ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @RequestBody OrderV1Dto.PlaceOrderRequest request
    );

    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 내역을 조회합니다.")
    @GetMapping
    ApiResponse<List<OrderV1Dto.OrderResponse>> getOrders(
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId
    );

    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(
            @Parameter(name = "X-USER-ID", description = "사용자 ID", required = true)
            @RequestHeader("X-USER-ID") String userId,

            @Parameter(description = "주문 ID", required = true)
            @PathVariable("orderId") Long orderId
    );
}
