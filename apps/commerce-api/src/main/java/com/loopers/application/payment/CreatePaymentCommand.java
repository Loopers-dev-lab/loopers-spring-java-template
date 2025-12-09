package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.interfaces.api.client.PaymentCreateV1Dto;

public record CreatePaymentCommand(Long userId, Long orderId, CardType cardType, String cardNo, Long amount, String callbackUrl) {

  public static CreatePaymentCommand from(Long userId, PaymentCreateV1Dto.PaymentRequest request) {
    return new CreatePaymentCommand(
        userId,
        request.orderId(),
        request.cardType(),
        request.cardNo(),
        request.amount(),
        request.callbackUrl()
    );
  }
}
