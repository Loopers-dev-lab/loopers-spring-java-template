package com.loopers.application.payment;

public interface PgClient {
  PgPayResponse requestPayment(PgPayRequest request);

  PgPaymentInfoResponse getPaymentInfo(String transactionKey);

  PgPaymentListResponse getPaymentsByOrder(String orderId);
}
