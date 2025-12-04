package com.loopers.infrastructure.payment;

public record PgPaymentResponse(
    String transactionKey,
    String status,
    String reason
) {
}
