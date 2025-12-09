package com.loopers.interfaces.api.client;

import com.loopers.application.payment.TransactionStatus;

public class PaymentCallbackV1Dto {

  public record CallbackRequest(
      String transactionKey,
      String orderId,
      String cardType,
      String cardNo,
      Long amount,
      TransactionStatus status,
      String reason
  ) {
    public static CallbackRequest success(String transactionKey, String orderId, String cardType, String cardNo, Long amount, TransactionStatus status) {
      return new CallbackRequest(transactionKey, orderId, cardType, cardNo, amount, status, "콜백 처리 성공");
    }

    public static CallbackRequest failure(String reason) {
      return new CallbackRequest(null, null, null, null, null, null, "콜백 처리 실패: " + reason);
    }
  }

  public record CallbackResponse(
      String reason
  ) {
    public static CallbackResponse success() {
      return new CallbackResponse("콜백 처리 성공");
    }

    public static CallbackResponse failure(String reason) {
      return new CallbackResponse("콜백 처리 실패: " + reason);
    }
  }
}
