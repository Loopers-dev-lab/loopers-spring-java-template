package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Order V1 API", description = "Order API 입니다.")
@RequestMapping("/api/v1/orders")
public interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "상품 주문을 생성합니다.")
    @PostMapping
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody OrderV1Dto.OrderCreateRequest request
    );

    @Operation(summary = "결제 콜백", description = "PG가 결제 결과를 전달합니다.")
    @PostMapping("/{orderReference}/callback")
    ApiResponse<Object> callback(
            @PathVariable("orderReference") String orderReference,
            @Valid @RequestBody OrderV1Dto.PaymentCallbackRequest request
    );

    @Operation(summary = "결제 상태 동기화", description = "콜백 누락 시 결제 상태를 수동으로 동기화합니다.")
    @PostMapping("/{orderId}/sync")
    ApiResponse<Object> syncPayment(
            @PathVariable("orderId") Long orderId
    );
}
