package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payment V1 API", description = "Payment V1 API 입니다.")
public interface PaymentV1ApiSpec {

    @Operation(
            summary = "결제 요청",
            description = "사용자가 결제 요청을 합니다."
    )
    ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(
            @Parameter(name = "X-USER-ID", description = "결제 요청을 하는 사용자의 로그인 ID", required = true)
            String loginId,

            @Schema(name = "결제 요청 정보", description = "결제할 정보")
            PaymentV1Dto.PaymentRequest request
    );

    @Operation(
            summary = "결제 콜백",
            description = "PG 시스템으로부터 결제 결과 콜백을 수신합니다."
    )
    ApiResponse<Object> handlePaymentCallback(
            @Schema(name = "결제 콜백 정보", description = "PG 시스템으로부터 받은 결제 결과 정보")
            PaymentV1Dto.PaymentCallbackRequest request
    );

}

