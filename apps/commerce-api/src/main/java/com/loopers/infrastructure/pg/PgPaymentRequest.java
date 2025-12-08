package com.loopers.infrastructure.pg;

import com.loopers.domain.payment.CardType;

public record PgPaymentRequest(
    String orderId,
    String cardType,
    String cardNo,
    Long amount,
    String callbackUrl
) {

  public static PgPaymentRequest of(
      Long orderId,
      CardType cardType,
      String cardNo,
      Long amount,
      String callbackUrl
  ) {
    return new PgPaymentRequest(
        String.valueOf(orderId),
        cardType.name(),
        cardNo,
        amount,
        callbackUrl
    );
  }
}
