package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payment V1 API", description = "Payment V1 API Spec")
public interface PaymentV1Spec {

    @Operation(summary = "PG사 결제 요청")
    ApiResponse<PaymentV1Dto.TransactionResponse> requestPayment(String userId, PaymentV1Dto.PaymentRequest request);

}
