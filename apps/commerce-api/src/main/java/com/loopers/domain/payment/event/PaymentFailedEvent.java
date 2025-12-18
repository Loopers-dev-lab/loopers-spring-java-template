package com.loopers.domain.payment.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PaymentFailedEvent(
    String eventId,
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
    Objects.requireNonNull(orderId, "orderId는 null일 수 없습니다.");
    Objects.requireNonNull(paymentId, "paymentId는 null일 수 없습니다.");
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
    Objects.requireNonNull(failedAt, "failedAt은 null일 수 없습니다.");
    return new PaymentFailedEvent(UUID.randomUUID().toString(), orderId, paymentId, userId, transactionKey, reason, failedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PAYMENT_FAILED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return failedAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(orderId);
  }
}
