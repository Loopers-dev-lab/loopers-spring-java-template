package com.loopers.domain.payment.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;

public record PaymentFailedEvent(
    Long orderId,
    Long paymentId,
    Long userId,
    String transactionKey,
    String reason,
    LocalDateTime failedAt
) implements DomainEvent {

  public static PaymentFailedEvent of(
      Long orderId,
      Long paymentId,
      Long userId,
      String transactionKey,
      String reason,
      LocalDateTime failedAt
  ) {
    return new PaymentFailedEvent(orderId, paymentId, userId, transactionKey, reason, failedAt);
  }

  @Override
  public String eventType() {
    return EventType.PAYMENT_FAILED.getCode();
  }

  @Override
  public LocalDateTime occurredAt() {
    return failedAt;
  }
}
