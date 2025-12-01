package com.loopers.application.api.payment;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.loopers.application.api.payment.PaymentV1Dto.*;

@Tag(name = "Payments V1 API", description = "결제 API 입니다.")
public interface PaymentsV1ApiSpec {

    @Operation(
            summary = "결제 요청",
            description = "결제 요청합니다."
    )
    ApiResponse<PaymentResponse> order(String userIdentifier, String orderId, PaymentRequest paymentRequest);

    @Operation(
            summary = "PG 결제 콜백",
            description = "PG 결제의 콜백 요청입니다."
    )
    ApiResponse<?> callback(PgCallbackRequest request);
}
