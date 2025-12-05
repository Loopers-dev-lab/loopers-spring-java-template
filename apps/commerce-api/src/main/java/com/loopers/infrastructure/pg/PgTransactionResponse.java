package com.loopers.infrastructure.pg;

import com.loopers.domain.payment.PaymentStatus;

public record PgTransactionResponse(
    String transactionKey,
    String orderId,
    String cardType,
    String cardNo,
    Long amount,
    String status,
    String reason
) {

  public boolean isSuccess() {
    return PaymentStatus.SUCCESS.isSameCode(status);
  }

  public boolean isFailed() {
    return PaymentStatus.FAILED.isSameCode(status);
  }

  public boolean isPending() {
    return PaymentStatus.PENDING.isSameCode(status);
  }
}
