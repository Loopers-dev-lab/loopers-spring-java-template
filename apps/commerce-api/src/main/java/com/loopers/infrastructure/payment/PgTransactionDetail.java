package com.loopers.infrastructure.payment;

public record PgTransactionDetail(
    String transactionKey,
    String orderId,
    String cardType,
    String cardNo,
    Long amount,
    String status,
    String reason
) {
}
