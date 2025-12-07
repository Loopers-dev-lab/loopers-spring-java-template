package com.loopers.infrastructure;

import com.loopers.interfaces.api.payment.PaymentV1Dto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientPgClient implements PgClient {
    private final PgClient pgClient;

    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "paymentFallback")
    @Retry(name = "pgRetry")
    public PaymentV1Dto.PgPaymentResponse requestPayment(PaymentV1Dto.PgPaymentRequest request) {
        return pgClient.requestPayment(request);
    }

    @Override
    public PaymentV1Dto.PgPaymentResponse getPaymentByTransactionKey(String transactionKey) {
        return pgClient.getPaymentByTransactionKey(transactionKey);
    }

    @Override
    public PaymentV1Dto.PgPaymentResponse getPaymentByOrderId(Long orderId) {
        return pgClient.getPaymentByOrderId(orderId);
    }

    public PaymentV1Dto.PgPaymentResponse paymentFallback(PaymentV1Dto.PgPaymentRequest request, Throwable t) {
        log.warn("PG CircuitBreaker fallback for orderId: {}, reason: {}", request.orderId(), t.getMessage());

        return new PaymentV1Dto.PgPaymentResponse(
                null, request.orderId(), PaymentV1Dto.pgPaymentStatus.FAILURE,
                "PG 서비스가 불안정하여 결제 요청에 실패했습니다. 잠시 후 다시 시도해주세요."
        );
    }
}
