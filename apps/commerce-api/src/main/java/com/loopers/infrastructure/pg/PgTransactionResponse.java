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
    if (status == null) return false;
    return PaymentStatus.SUCCESS.isSameCode(status);
  }

  public boolean isFailed() {
    if (status == null) return false;
    return PaymentStatus.FAILED.isSameCode(status);
  }

  public boolean isPending() {
    if (status == null) return false;
    return PaymentStatus.PENDING.isSameCode(status);
  }
}
