package com.loopers.infrastructure.pg;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PgService {

    private final PgClient pgClient;

    /**
     * PG 결제 요청
     * @return transactionId (거래 ID)
     */
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    public String requestPayment(String userId, String orderId, String cardType,
                                 String cardNo, String amount, String callbackUrl) {
        log.info("PG 결제 요청: orderId={}, amount={}", orderId, amount);

        PgPaymentRequest request = PgPaymentRequest.of(
                orderId, cardType, cardNo, amount, callbackUrl
        );

        PgPaymentResponse response = pgClient.requestPayment(userId, request);

        log.info("PG 결제 요청 성공: transactionId={}", response.transactionId());

        return response.transactionId();
    }

    /**
     * PG 결제 상태 조회
     */
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentDetailFallback")
    @Retry(name = "pgRetry")
    public PgPaymentResponse getPaymentDetail(String userId, String transactionId) {
        log.info("PG 결제 상태 조회: transactionId={}", transactionId);

        PgPaymentResponse response = pgClient.getPaymentDetail(userId, transactionId);

        log.info("PG 결제 상태: transactionId={}, status={}",
                transactionId, response.status());

        return response;
    }

    /**
     * 결제 요청 Fallback
     */
    private String requestPaymentFallback(String userId, String orderId, String cardType,
                                          String cardNo, String amount, String callbackUrl,
                                          Throwable t) {
        log.error("PG 결제 요청 Fallback 실행: orderId={}, error={}",
                orderId, t.getMessage());

        throw new com.loopers.support.error.CoreException(
                com.loopers.support.error.ErrorType.INTERNAL_ERROR,
                "결제 시스템이 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."
        );
    }

    /**
     * 결제 상태 조회 Fallback
     * - 조회 실패 시 null 반환 (스케줄러에서 다시 시도)
     */
    private PgPaymentResponse getPaymentDetailFallback(String userId, String transactionId,
                                                       Throwable t) {
        log.error("PG 결제 상태 조회 Fallback: transactionId={}, error={}",
                transactionId, t.getMessage());

        // null 반환 시 스케줄러에서 재시도
        return null;
    }
}
