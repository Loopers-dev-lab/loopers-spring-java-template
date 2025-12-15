package com.loopers.application.payment;

import com.loopers.domain.order.Money;
import com.loopers.domain.payment.CardType;

public interface PgClient {
  PgPayResponse requestPayment(Long orderId, CardType cardType, String cardNo, Money price);

  PgPaymentInfoResponse getPaymentInfo(String transactionKey);

  PgPaymentListResponse getPaymentsByOrder(String orderId);
}
