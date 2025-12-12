package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderRequest;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Order V1 API", description = "주문 API 입니다.")
public interface OrderV1ApiSpec {
    // /api/v1/orders - POST
    @Operation(
            method = "POST",
            summary = "주문 생성",
            description = "새로운 주문을 생성합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Schema(
                    name = "주문 요청 정보",
                    description = "주문 생성에 필요한 정보"
            )
            OrderRequest request
    );

    // /api/v1/orders/{orderId}/pay - POST
    @Operation(
            method = "POST",
            summary = "주문 결제",
            description = "특정 주문에 대한 결제를 처리합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> payOrder(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Schema(
                    name = "주문 ID",
                    description = "결제할 주문의 ID"
            )
            Long orderId,
            @Schema(
                    name = "결제 요청 정보",
                    description = "주문 결제에 필요한 정보"
            )
            OrderRequest.PaymentRequest request
    );

    // /api/v1/orders - GET
    @Operation(
            method = "GET",
            summary = "주문 목록 조회",
            description = "회원의 주문 목록을 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderPageResponse> getOrderList(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PageableDefault(size = 20) Pageable pageable
    );

    // /api/v1/orders/{orderId} - GET
    @Operation(
            method = "GET",
            summary = "주문 상세 조회",
            description = "특정 주문의 상세 정보를 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Schema(
                    name = "주문 ID",
                    description = "조회할 주문의 ID"
            )
            Long orderId
    );
}
