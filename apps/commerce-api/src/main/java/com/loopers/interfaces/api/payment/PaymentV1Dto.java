package com.loopers.interfaces.api.payment;

public class PaymentV1Dto {
    public record TransactionResponse(
            String transactionKey,
            TransactionStatusResponse status,
            String reason
    ) { }

    public enum TransactionStatusResponse {
        PENDING,
        SUCCESS,
        FAILED,
    }

    public record PgPaymentRequest(
            String orderId,
            CardTypeDto cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) { }

    public enum CardTypeDto {
        SAMSUNG,
        KB,
        HYUNDAI
    }

    public record PaymentRequest(String userId, String orderNo, String transactionKey) {
    }
}
