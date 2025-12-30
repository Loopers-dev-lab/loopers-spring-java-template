package com.loopers.interfaces.api.payment;

import com.loopers.domain.payment.PaymentDto;

/**
 * Payment API DTO
 */
public class PaymentApiDto {

    /**
     * PG Simulator 콜백 요청 DTO
     * pg-simulator가 callbackUrl로 POST로 보내는 TransactionInfo 객체
     * (ApiResponse로 감싸지 않고 직접 전송)
     */
    public record PgCallbackRequest(
            String transactionKey,
            String orderId,
            PaymentDto.CardType cardType,
            String cardNo,
            Long amount,
            PaymentDto.PaymentStatus status,
            String reason
    ) {}

    /**
     * Payment 응답 DTO
     */
    public record PaymentResponse(
            Long orderId,
            String transactionKey,
            PaymentDto.PaymentStatus status,
            String reason
    ) {}
}

