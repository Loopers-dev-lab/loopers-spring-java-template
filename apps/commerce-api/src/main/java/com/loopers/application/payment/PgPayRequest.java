package com.loopers.application.payment;

import java.math.BigDecimal;

public record PgPayRequest(
    Long orderId,
    String cardType,
    String cardNo,
    BigDecimal amount,
    String callbackUrl
) {
    public PgPayRequest(Long orderId, String cardType, String cardNo, BigDecimal amount) {
        this(orderId, cardType, cardNo, amount, "http://localhost:8080/api/v1/payments/callback");
    }
}
