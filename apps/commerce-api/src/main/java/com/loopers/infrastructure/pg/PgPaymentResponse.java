package com.loopers.infrastructure.pg;

import com.loopers.domain.payment.PaymentStatus;

public record PgPaymentResponse(
    String transactionKey,
    String status,
    String reason
) {

  public boolean isPending() {
    return PaymentStatus.PENDING.isSameCode(status);
  }
}
