package com.loopers.interfaces.api.payment;

import java.math.BigDecimal;

public record PaymentRequest(
    String orderId,
    String cardType,
    String cardNo,
    BigDecimal amount
) {
}
