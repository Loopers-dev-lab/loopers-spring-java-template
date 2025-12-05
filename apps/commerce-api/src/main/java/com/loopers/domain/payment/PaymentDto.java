package com.loopers.domain.payment;

import lombok.Builder;

/**
 * PG Simulator 통신용 DTO
 */
public class PaymentDto {

    /**
     * PG Simulator 결제 요청 DTO
     * POST /api/v1/payments 요청
     */
    @Builder
    public record PgRequest(
            String orderId,
            CardType cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {}

    /**
     * PG Simulator 결제 응답 DTO (data 부분)
     * POST /api/v1/payments 응답: ApiResponse<TransactionResponse>의 data 필드
     */
    @Builder
    public record PgResponse(
            String transactionKey,
            PaymentStatus status,
            String reason
    ) {}

    /**
     * 결제 타입
     */
    public enum PaymentMethod {
        POINT, CARD, BANK_TRANSFER
    }

    /**
     * 카드 타입 Enum (PG Simulator와 호환)
     */
    public enum CardType {
        SAMSUNG,
        KB,
        HYUNDAI
    }

    /**
     * 결제 상태
     */
    public enum PaymentStatus {
        PENDING,       // 결제 대기
        SUCCESS,       // 결제 완료
        FAILED,        // 결제 실패
        CANCELLED,     // 결제 취소
        REFUNDED       // 환불됨
    }

}

