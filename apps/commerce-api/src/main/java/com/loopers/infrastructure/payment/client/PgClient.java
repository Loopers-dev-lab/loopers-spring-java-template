package com.loopers.infrastructure.payment.client;

import com.loopers.infrastructure.payment.client.dto.PgClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * PG Simulator와 통신하는 FeignClient
 * - Timeout: 연결 1초, 응답 3초 (application.yml)
 * - CircuitBreaker와 Retry는 PgClientService에서 적용
 */
@FeignClient(
        name = "pg-client",
        url = "${pg.url}",
        configuration = PgClientConfig.class
)
public interface PgClient {

    /**
     * 결제 요청
     *
     * @param userId  사용자 ID (헤더)
     * @param request 결제 요청 정보
     * @return 트랜잭션 응답
     */
    @PostMapping("/api/v1/payments")
    ApiResponse<PgClientDto.TransactionResponse> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgClientDto.PaymentRequest request
    );

    /**
     * 트랜잭션 상세 조회
     *
     * @param userId         사용자 ID (헤더)
     * @param transactionKey 트랜잭션 키
     * @return 트랜잭션 상세 정보
     */
    @GetMapping("/api/v1/payments/{transactionKey}")
    ApiResponse<PgClientDto.TransactionDetailResponse> getTransaction(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable("transactionKey") String transactionKey
    );

    /**
     * 주문별 트랜잭션 목록 조회
     *
     * @param userId  사용자 ID (헤더)
     * @param orderId 주문 ID
     * @return 주문별 트랜잭션 목록
     */
    @GetMapping("/api/v1/payments")
    ApiResponse<PgClientDto.OrderResponse> getTransactionsByOrder(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );

    /**
     * PG API 응답 포맷
     */
    record ApiResponse<T>(
            boolean success,
            T data,
            String errorCode,
            String message
    ) {
    }
}
