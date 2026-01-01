package com.loopers.interfaces.api.client;

import com.loopers.domain.payment.CardType;

public class PaymentCreateV1Dto {
  public record PaymentRequest(String orderId, CardType cardType, String cardNo, Long amount, String callbackUrl) {
  }

}
