package com.loopers.application.payment;

public record PgPayRequest(
    String orderId,
    String cardType,
    String cardNo,
    Long amount,
    String callbackUrl
) {
}