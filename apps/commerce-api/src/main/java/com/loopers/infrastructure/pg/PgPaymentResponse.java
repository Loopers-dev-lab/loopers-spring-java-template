package com.loopers.infrastructure.pg;

public record PgPaymentResponse(
        String transactionId,
        String status,
        String message
) {
}
