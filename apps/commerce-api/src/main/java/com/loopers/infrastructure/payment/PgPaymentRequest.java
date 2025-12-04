package com.loopers.infrastructure.payment;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PgPaymentRequest(
    String orderId,
    String cardType,
    String cardNo,
    Long amount,
    String callbackUrl
) {
}
