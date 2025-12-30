package com.loopers.infrastructure.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Collections;
import java.util.List;

public class PgPaymentV1Dto {

    public enum TransactionStatus {
        SUCCESS,
        FAILED,
        PENDING
    }

    public record Request(
            @NotBlank(message = "주문 ID는 필수입니다")
            String orderId,
            @NotBlank(message = "카드 타입은 필수입니다")
            String cardType,
            @NotBlank(message = "카드 번호는 필수입니다")
            String cardNo,
            @NotNull(message = "결제 금액은 필수입니다")
            @Positive(message = "결제 금액은 0보다 커야 합니다")
            Long amount,
            @NotBlank(message = "콜백 URL은 필수입니다")
            String callbackUrl
    ) {
    }

    public record Response(
            String transactionKey,
            PgPaymentV1Dto.TransactionStatus status,
            String reason
    ) {
    }

    public record OrderResponse(
            String orderId,
            List<TransactionRecord> transactions
    ) {
        public OrderResponse(String orderId) {
            this(orderId, Collections.emptyList());
        }
    }

    public record TransactionRecord(
            String transactionKey,
            PgPaymentV1Dto.TransactionStatus status,
            String reason
    ) {
    }
}
