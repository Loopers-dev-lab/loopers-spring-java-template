package com.loopers.interfaces.api.payment;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public class PaymentV1Dtos {
    public record PaymentRequest(
            Long orderId,
            String cardType,
            String cardNo,
            BigDecimal amount,
            String callbackUrl
    ) {
    }

    /**
     * PG-Simulator 콜백 요청 DTO
     * <p>
     * PG-Simulator의 TransactionInfo 구조와 일치시킴
     * - transactionKey: 트랜잭션 키
     * - orderNumber: 주문 ID
     * - cardType: 카드 타입
     * - cardNo: 카드 번호
     * - amount: 결제 금액
     * - status: 결제 상태 (SUCCESS, FAILED, PENDING)
     * - reason: 실패 사유 (nullable)
     */
    public record PgCallbackRequest(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String status,
            String reason
    ) {
    }
}

