package com.loopers.infrastructure.payment.client;

import com.loopers.infrastructure.payment.client.dto.PgClientDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PG Client를 호출하는 서비스 - CircuitBreaker: 외부 시스템 장애 시 차단 - Retry: 일시적 실패 시 재시도 - Fallback: 최종 실패 시 대체 응답
 */
@Service
public class PgClientService {

    private static final Logger log = LoggerFactory.getLogger(PgClientService.class);

    private final PgClient pgClient;

    public PgClientService(PgClient pgClient) {
        this.pgClient = pgClient;
    }

    /**
     * 결제 요청 (CircuitBreaker + Retry 적용)
     *
     * @param userId  사용자 ID
     * @param request 결제 요청 정보
     * @return 트랜잭션 응답
     */
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    public PgClient.ApiResponse<PgClientDto.TransactionResponse> requestPayment(
        String userId,
        PgClientDto.PaymentRequest request
    ) {
        log.info("PG 결제 요청: userId={}, orderId={}", userId, request.orderId());
        return pgClient.requestPayment(userId, request);
    }

    /**
     * 트랜잭션 상세 조회 (CircuitBreaker 적용)
     *
     * @param userId         사용자 ID
     * @param transactionKey 트랜잭션 키
     * @return 트랜잭션 상세 정보
     */
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getTransactionFallback")
    public PgClient.ApiResponse<PgClientDto.TransactionDetailResponse> getTransaction(
        String userId,
        String transactionKey
    ) {
        log.info("PG 트랜잭션 조회: userId={}, transactionKey={}", userId, transactionKey);
        return pgClient.getTransaction(userId, transactionKey);
    }

    /**
     * 주문별 트랜잭션 목록 조회 (CircuitBreaker 적용)
     *
     * @param userId  사용자 ID
     * @param orderId 주문 ID
     * @return 주문별 트랜잭션 목록
     */
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getTransactionsByOrderFallback")
    public PgClient.ApiResponse<PgClientDto.OrderResponse> getTransactionsByOrder(
        String userId,
        String orderId
    ) {
        log.info("PG 주문별 트랜잭션 조회: userId={}, orderId={}", userId, orderId);
        return pgClient.getTransactionsByOrder(userId, orderId);
    }

    // ========== Fallback Methods ==========

    /**
     * 결제 요청 실패 시 Fallback - PG 시스템 장애 시 PENDING 상태로 반환 - 나중에 결제 상태 확인 API로 재확인 가능
     */
    private PgClient.ApiResponse<PgClientDto.TransactionResponse> requestPaymentFallback(
        String userId,
        PgClientDto.PaymentRequest request,
        Throwable t
    ) {
        log.error("PG 결제 요청 실패 (Fallback 처리): userId={}, orderId={}, error={}",
            userId, request.orderId(), t.getMessage());

        // Fallback: PENDING 상태로 반환 (나중에 상태 확인 필요)
        PgClientDto.TransactionResponse fallbackResponse = new PgClientDto.TransactionResponse(
            null,  // transactionKey는 null (생성 실패)
            PgClientDto.TransactionStatus.PENDING,
            "PG 시스템 일시 장애로 결제 대기 중입니다. 잠시 후 다시 확인해주세요."
        );

        return new PgClient.ApiResponse<>(false, fallbackResponse, "FALLBACK", "PG 시스템 장애");
    }

    /**
     * 트랜잭션 조회 실패 시 Fallback
     */
    private PgClient.ApiResponse<PgClientDto.TransactionDetailResponse> getTransactionFallback(
        String userId,
        String transactionKey,
        Throwable t
    ) {
        log.error("PG 트랜잭션 조회 실패 (Fallback 처리): userId={}, transactionKey={}, error={}",
            userId, transactionKey, t.getMessage());

        // Fallback: PENDING 상태로 반환
        PgClientDto.TransactionDetailResponse fallbackResponse = new PgClientDto.TransactionDetailResponse(
            transactionKey,
            null,
            null,
            null,
            null,
            PgClientDto.TransactionStatus.PENDING,
            "PG 시스템 일시 장애로 조회할 수 없습니다."
        );

        return new PgClient.ApiResponse<>(false, fallbackResponse, "FALLBACK", "PG 시스템 장애");
    }

    /**
     * 주문별 트랜잭션 조회 실패 시 Fallback
     */
    private PgClient.ApiResponse<PgClientDto.OrderResponse> getTransactionsByOrderFallback(
        String userId,
        String orderId,
        Throwable t
    ) {
        log.error("PG 주문별 트랜잭션 조회 실패 (Fallback 처리): userId={}, orderId={}, error={}",
            userId, orderId, t.getMessage());

        // Fallback: 빈 목록 반환
        PgClientDto.OrderResponse fallbackResponse = new PgClientDto.OrderResponse(
            orderId,
            java.util.Collections.emptyList()
        );

        return new PgClient.ApiResponse<>(false, fallbackResponse, "FALLBACK", "PG 시스템 장애");
    }
}
