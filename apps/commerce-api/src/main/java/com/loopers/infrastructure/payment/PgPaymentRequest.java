package com.loopers.infrastructure.payment;

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
