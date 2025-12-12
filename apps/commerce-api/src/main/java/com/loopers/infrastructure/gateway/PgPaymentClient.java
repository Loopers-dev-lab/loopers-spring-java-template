package com.loopers.infrastructure.gateway;

import com.loopers.domain.payment.PaymentClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgPaymentClient implements PaymentClient {

    @Value("${pg-simulator.callback-url}")
    private String callbackUrl;

    private final PgPaymentFeignClient pgPaymentFeignClient;

    @Override
    @Retry(name = "pgRetry", fallbackMethod = "requestPaymentRetryFallback")
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "requestPaymentFallback")
    public PaymentClient.PaymentResponse requestPayment(PaymentClient.PaymentRequest request) {

        PgPaymentDto.PgPaymentRequest pgRequest = new PgPaymentDto.PgPaymentRequest(
                request.orderId(),
                request.cardType(),
                request.cardNo(),
                (long) request.amount(),
                callbackUrl
        );

        PgPaymentDto.PgPaymentResponse response = pgPaymentFeignClient.requestPayment(request.userId(), pgRequest);

        return PaymentClient.PaymentResponse.createPaymentResponse(
                response.data().transactionKey(),
                response.data().status(),
                response.data().reason()
        );
    }

    public PaymentClient.PaymentResponse requestPaymentRetryFallback(PaymentClient.PaymentRequest request, Throwable t) {
        log.warn("PG 결제 요청 Retry Fallback 호출: orderKey={}, error={}", request.orderId(), t.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("결제 요청이 재시도 후에도 실패했습니다.");
    }

    public PaymentClient.PaymentResponse requestPaymentFallback(PaymentClient.PaymentRequest request, Exception e) {
        log.error("PG 결제 요청 CircuitBreaker Fallback 호출: orderKey={}, error={}", request.orderId(), e.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("PG 시스템이 일시적으로 사용할 수 없습니다.");
    }

    @Override
    @Retry(name = "pgRetry", fallbackMethod = "getPaymentStatusRetryFallback")
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "getPaymentStatusFallback")
    public PaymentClient.PaymentResponse getPaymentStatusByTransactionKey(PaymentClient.PaymentStatusRequest request) {
        PgPaymentDto.PgPaymentResponse response = pgPaymentFeignClient.getPaymentStatus(
                request.userId(),
                request.transactionKey()
        );

        return PaymentClient.PaymentResponse.createPaymentResponse(
                response.data().transactionKey(),
                response.data().status(),
                response.data().reason()
        );
    }

    public PaymentClient.PaymentResponse getPaymentStatusRetryFallback(PaymentClient.PaymentStatusRequest request, Throwable t) {
        log.warn("PG 결제 상태 조회 Retry Fallback 호출: transactionKey={}, error={}", request.transactionKey(), t.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("결제 상태 조회가 재시도 후에도 실패했습니다.");
    }

    public PaymentClient.PaymentResponse getPaymentStatusFallback(PaymentClient.PaymentStatusRequest request, Exception e) {
        log.error("PG 결제 상태 조회 CircuitBreaker Fallback 호출: transactionKey={}, error={}", request.transactionKey(), e.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("PG 시스템이 일시적으로 사용할 수 없습니다.");
    }

    @Override
    @Retry(name = "pgRetry", fallbackMethod = "getPaymentStatusByOrderKeyRetryFallback")
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "getPaymentStatusByOrderKeyFallback")
    public PaymentClient.PaymentResponse getPaymentStatusByOrderKey(PaymentClient.PaymentStatusByOrderKeyRequest request) {
        PgPaymentDto.PgPaymentOrderResponse response = pgPaymentFeignClient.getPaymentStatusByOrderKey(
                request.userId(),
                request.orderKey()
        );

        if (response.hasTransactionKey()) {
            return PaymentClient.PaymentResponse.createFailResponse("결제 내역을 찾을 수 없습니다.");
        }

        PgPaymentDto.PgPaymentOrderResponse.TransactionData latestTransaction = response.data().transactions().get(0);

        return PaymentClient.PaymentResponse.createPaymentResponse(
                latestTransaction.transactionKey(),
                latestTransaction.status(),
                latestTransaction.reason()
        );
    }

    public PaymentClient.PaymentResponse getPaymentStatusByOrderKeyRetryFallback(PaymentClient.PaymentStatusByOrderKeyRequest request, Throwable t) {
        log.warn("PG 결제 상태 조회(OrderKey) Retry Fallback 호출: orderKey={}, error={}", request.orderKey(), t.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("결제 상태 조회가 재시도 후에도 실패했습니다.");
    }

    public PaymentClient.PaymentResponse getPaymentStatusByOrderKeyFallback(PaymentClient.PaymentStatusByOrderKeyRequest request, Exception e) {
        log.error("PG 결제 상태 조회(OrderKey) CircuitBreaker Fallback 호출: orderKey={}, error={}", request.orderKey(), e.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("PG 시스템이 일시적으로 사용할 수 없습니다.");
    }
}

