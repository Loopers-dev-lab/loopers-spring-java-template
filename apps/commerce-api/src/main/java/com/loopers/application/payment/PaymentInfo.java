package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

import java.math.BigDecimal;

public record PaymentInfo(
    String paymentId,
    Long orderId,
    CardType cardType,
    String cardNo,
    BigDecimal amount,
    PaymentStatus status,
    String transactionKey
) {

  public static PaymentInfo from(Payment payment) {
    return new PaymentInfo(
        payment.getId().toString(),
        payment.getOrderId(),
        payment.getCardType(),
        payment.getCardNo(),
        payment.getAmount().getAmount(),
        payment.getStatus(),
        payment.getTransactionKey()
    );
  }
}
