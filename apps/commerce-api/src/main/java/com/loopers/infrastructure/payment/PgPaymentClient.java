package com.loopers.infrastructure.payment;

import com.loopers.infrastructure.payment.dto.PgPaymentV1Dto;
import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "pgPaymentClient",
        url = "${pg.client.base-url:http://localhost:8081}"
)
public interface PgPaymentClient {

    @PostMapping("/api/v1/payments")
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallback")
    @Retry(name = "pgRetry")
    ApiResponse<PgPaymentV1Dto.Response> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentV1Dto.Request request
    );

    @GetMapping("/api/v1/payments")
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentsFallback")
    @Retry(name = "pgRetry")
    ApiResponse<PgPaymentV1Dto.OrderResponse> getPayments(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );

    default ApiResponse<PgPaymentV1Dto.Response> fallback(String userId, PgPaymentV1Dto.Request request, Throwable throwable) {
        PgPaymentV1Dto.Response response = new PgPaymentV1Dto.Response(
                null,
                PgPaymentV1Dto.TransactionStatus.FAILED,
                throwable.getMessage()
        );
        return new ApiResponse<>(
                ApiResponse.Metadata.fail("PG_ERROR", throwable.getMessage()),
                response
        );
    }

    default ApiResponse<PgPaymentV1Dto.OrderResponse> getPaymentsFallback(String userId, String orderId, Throwable throwable) {
        PgPaymentV1Dto.OrderResponse response = new PgPaymentV1Dto.OrderResponse(orderId, Collections.emptyList());
        return new ApiResponse<>(
                ApiResponse.Metadata.fail("PG_ERROR", throwable.getMessage()),
                response
        );
    }
}
