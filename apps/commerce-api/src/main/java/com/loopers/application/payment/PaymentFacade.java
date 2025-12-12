package com.loopers.application.payment;

import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFacade {
    private final PgClient pgClient;

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallback")
    public PaymentV1Dto.TransactionResponse requestPayment(String userId, PaymentV1Dto.PaymentRequest request) {
        ApiResponse<PaymentV1Dto.TransactionResponse> result = pgClient.requestPayment(userId, request);

        PaymentV1Dto.TransactionResponse response = result.data();

        return response;
    }

    public PaymentV1Dto.TransactionResponse fallback(String userId, PaymentV1Dto.PaymentRequest request, Throwable throwable) {
        return new PaymentV1Dto.TransactionResponse(
                null,
                PaymentV1Dto.TransactionStatusResponse.FAILED,
                "현재 결제 서비스가 불안정합니다. 잠시 후 다시 시도해주세요."
        );
    }
}
