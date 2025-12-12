package com.loopers.infrastructure.payment.client.dto;

/**
 * PG Simulator와 통신하기 위한 DTO
 */
public class PgClientDto {

    /**
     * PG 결제 요청 DTO
     */
    public record PaymentRequest(
            String orderId,
            CardType cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {
    }

    /**
     * PG 트랜잭션 응답 DTO
     */
    public record TransactionResponse(
            String transactionKey,
            TransactionStatus status,
            String reason
    ) {
    }

    /**
     * PG 트랜잭션 상세 응답 DTO
     */
    public record TransactionDetailResponse(
            String transactionKey,
            String orderId,
            CardType cardType,
            String cardNo,
            Long amount,
            TransactionStatus status,
            String reason
    ) {
    }

    /**
     * PG 주문별 트랜잭션 목록 응답 DTO
     */
    public record OrderResponse(
            String orderId,
            java.util.List<TransactionResponse> transactions
    ) {
    }

    /**
     * 카드 타입
     */
    public enum CardType {
        SAMSUNG, KB, HYUNDAI
    }

    /**
     * 트랜잭션 상태
     */
    public enum TransactionStatus {
        PENDING,  // 대기
        SUCCESS,  // 성공
        FAILED    // 실패
    }
}
