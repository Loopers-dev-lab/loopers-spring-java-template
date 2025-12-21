package com.loopers.domain.payment.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSucceededEvent(
    String eventId,
    Long orderId,
    Long paymentId,
    Long userId,
    String transactionKey,
    Long pgAmount,
    LocalDateTime completedAt
) implements DomainEvent {

  public static PaymentSucceededEvent of(
      Long orderId,
      Long paymentId,
      Long userId,
      String transactionKey,
      Long pgAmount,
      LocalDateTime completedAt
  ) {
    return new PaymentSucceededEvent(UUID.randomUUID().toString(), orderId, paymentId, userId, transactionKey, pgAmount, completedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PAYMENT_SUCCEEDED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return completedAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(orderId);
  }
}
