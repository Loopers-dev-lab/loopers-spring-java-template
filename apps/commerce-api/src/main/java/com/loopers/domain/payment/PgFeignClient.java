package com.loopers.domain.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "pgClient", url = "${pg.api.url}")
public interface PgFeignClient {

    @Retry(name = "pg-api")
    @CircuitBreaker(name = "pg-api")
    @RateLimiter(name = "pg-api")
    @PostMapping("/api/v1/payments")
    ApiResponse<PaymentDto.PgResponse> approvePayment(
            @RequestHeader("X-USER-ID") Long userId,
            PaymentDto.PgRequest request
    );
}
