package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Payment V1 API 명세
 */
@Tag(name = "Payment API", description = "결제 API")
public interface PaymentV1ApiSpec {

    @Operation(summary = "결제 요청", description = "PG를 통한 결제를 요청합니다")
    ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(
            @RequestHeader(value = "X-USER-ID") String userId,
            @RequestBody PaymentV1Dto.PaymentRequest request
    );

    @Operation(summary = "결제 상세 조회", description = "결제 정보를 조회합니다")
    ApiResponse<PaymentV1Dto.PaymentResponse> getPayment(
            @RequestHeader(value = "X-USER-ID") String userId,
            @PathVariable("paymentId") Long paymentId
    );

    @Operation(summary = "PG 콜백 수신", description = "PG로부터 결제 결과를 수신합니다")
    ApiResponse<Void> handleCallback(
            @RequestBody PaymentV1Dto.CallbackRequest request
    );
}
