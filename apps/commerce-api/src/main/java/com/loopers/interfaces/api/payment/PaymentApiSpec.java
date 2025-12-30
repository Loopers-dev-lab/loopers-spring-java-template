package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payment API", description = "결제 관련 API 입니다.")
public interface PaymentApiSpec {

    @Operation(
        summary = "결제 콜백 처리",
        description = "PG Simulator로부터 결제 처리 결과를 콜백으로 받습니다. " +
                     "콜백 URL: /api/v1/payments/callback"
    )
    ApiResponse<PaymentApiDto.PaymentResponse> callbackPayment(
        @Schema(name = "결제 콜백 요청", description = "PG Simulator가 보내는 결제 결과 정보")
        PaymentApiDto.PgCallbackRequest request
    );
}

