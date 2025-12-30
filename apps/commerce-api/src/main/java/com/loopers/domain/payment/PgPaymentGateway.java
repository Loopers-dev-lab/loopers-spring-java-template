package com.loopers.domain.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PgPaymentGateway.class);
    
    private final PgFeignClient pgFeignClient;

    @Retry(name = "pg-api")
    @RateLimiter(name = "pg-api")
    @CircuitBreaker(name = "pg-api", fallbackMethod = "fallbackApprovePayment")
    public ApiResponse<PaymentDto.PgResponse> approvePayment(Long userId, PaymentDto.PgRequest request) {
        return pgFeignClient.approvePayment(userId, request);
    }

    @SuppressWarnings("unused")
    private ApiResponse<PaymentDto.PgResponse> fallbackApprovePayment(Long userId, PaymentDto.PgRequest request, Throwable t) {
        log.warn("PG API Circuit Breaker is open or an error occurred. Fallback approvePayment is called. userId: {}, orderId: {}, error: {}", 
                 userId, request.orderId(), t.getMessage());

        // 서킷이 열렸을 때, PG사 시스템 장애로 간주하고 실패 응답을 반환합니다.
        PaymentDto.PgResponse failedResponse = PaymentDto.PgResponse.builder()
                .status(PaymentDto.PaymentStatus.FAILED)
                .reason("PG사 시스템 장애로 인해 결제가 불가능합니다.")
                .build();
        
        return ApiResponse.success(failedResponse);
    }
}
