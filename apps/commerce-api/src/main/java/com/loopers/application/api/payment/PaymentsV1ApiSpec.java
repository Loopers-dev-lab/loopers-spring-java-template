package com.loopers.application.api.payment;

import com.loopers.application.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.loopers.application.api.payment.PaymentV1Dto.PaymentRequest;
import static com.loopers.application.api.payment.PaymentV1Dto.PaymentResponse;

@Tag(name = "Payments V1 API", description = "결재 API 입니다.")
public interface PaymentsV1ApiSpec {

    @Operation(
            summary = "결재 요청",
            description = "결재 요청합니다."
    )
    ApiResponse<PaymentResponse> order(String userIdentifier, String orderId, PaymentRequest paymentRequest);
}
