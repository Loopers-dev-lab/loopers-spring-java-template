package com.loopers.infrastructure.pg;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * PG 호출을 전담하며 서킷브레이커/재시도를 관리한다.
 * <p>
 * 실행 순서:
 * 1. CircuitBreaker 체크 (서킷 상태 확인)
 * 2. Retry 시작 (최대 3회 시도)
 * 3. 실제 PG 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgPaymentExecutor {

    private final PgClient pgClient;

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentAsyncFallback")
    @Retry(name = "pgRetry")
    public CompletableFuture<PgV1Dto.PgTransactionResponse> requestPaymentAsync(PgV1Dto.PgPaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("PG 결제 요청 시작 - orderId: {}", request.orderId());

            ApiResponse<PgV1Dto.PgTransactionResponse> response = pgClient.requestPayment(request);

            log.info("PG 결제 요청 응답 수신 - orderId: {}, response: {}", request.orderId(), response);

            if (response.meta().result() == ApiResponse.Metadata.Result.FAIL) {
                throw new CoreException(ErrorType.BAD_REQUEST, "PG 결제 요청 실패: " + response.meta().message());
            }

            return response.data();
        });
    }

    public CompletableFuture<PgV1Dto.PgTransactionResponse> requestPaymentAsyncFallback(PgV1Dto.PgPaymentRequest request, Throwable t) {

        log.warn("PG 결제 요청 실패 (CircuitBreaker Fallback) - orderId: {}, cause: {}",
                request.orderId(), t.getMessage(), t);

        return CompletableFuture.completedFuture(
                new PgV1Dto.PgTransactionResponse(
                        null,
                        "PENDING",
                        "외부 시스템 오류로 결제 요청이 처리되지 않았습니다."
                )
        );
    }

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentDetailByTransactionKeyFallback")
    @Retry(name = "pgRetry")
    public CompletableFuture<PgV1Dto.PgTransactionDetailResponse> getPaymentDetailByTransactionKeyAsync(String transactionKey) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("PG 결제 상세 조회 시작 - transactionKey: {}", transactionKey);

            ApiResponse<PgV1Dto.PgTransactionDetailResponse> response = pgClient.getTransactionDetail(transactionKey);

            log.info("PG 결제 상세 조회 응답 수신 - transactionKey: {}, response: {}", transactionKey, response);

            if (response.meta().result() == ApiResponse.Metadata.Result.FAIL) {
                throw new CoreException(ErrorType.BAD_REQUEST, "PG 결제 상세 조회 실패: " + response.meta().message());
            }

            return response.data();
        });
    }

    public CompletableFuture<PgV1Dto.PgTransactionDetailResponse> getPaymentDetailByTransactionKeyFallback(String transactionKey, Throwable t) {
        log.warn("PG 결제 상세 조회 실패 (CircuitBreaker Fallback) - transactionKey: {}, cause: {}",
                transactionKey, t.getMessage(), t);
        return CompletableFuture.completedFuture(
                new PgV1Dto.PgTransactionDetailResponse(
                        transactionKey,
                        null,
                        null,
                        null,
                        null,
                        "PENDING",
                        "외부 시스템 오류로 결제 상세 조회가 처리되지 않았습니다."
                )
        );
    }

    // ==================== 주문별 결제 조회 ====================

    /**
     * 주문 ID로 결제 정보 조회 (비동기)
     * 동기 메서드를 비동기로 래핑
     */
    public CompletableFuture<PgV1Dto.PgOrderResponse> getPaymentByOrderIdAsync(String orderId) {
        return CompletableFuture.supplyAsync(() -> getPaymentByOrderIdSync(orderId));
    }

    /**
     * 주문 ID로 결제 정보 조회 (동기)
     * CircuitBreaker와 Retry가 적용됨
     */
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentByOrderIdFallback")
    @Retry(name = "pgRetry")
    public PgV1Dto.PgOrderResponse getPaymentByOrderIdSync(String orderId) {
        ApiResponse<PgV1Dto.PgOrderResponse> response = pgClient.getPaymentByOrderId(orderId);

        if (response.meta().result() == ApiResponse.Metadata.Result.FAIL) {
            throw new CoreException(ErrorType.BAD_REQUEST, "PG 주문 조회 실패: " + response.meta().message());
        }

        return response.data();
    }

    /**
     * CircuitBreaker Fallback - 서킷 오픈 또는 최종 실패 시
     */
    public PgV1Dto.PgOrderResponse getPaymentByOrderIdFallback(String orderId, Throwable t) {
        log.warn("PG 주문 조회 실패 (CircuitBreaker Fallback) - orderId: {}, cause: {}",
                orderId, t.getMessage(), t);

        return new PgV1Dto.PgOrderResponse(
                orderId,
                List.of(),
                true
        );
    }
}
