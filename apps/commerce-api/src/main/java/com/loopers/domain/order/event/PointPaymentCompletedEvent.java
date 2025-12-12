package com.loopers.domain.order.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;

public record PointPaymentCompletedEvent(
    Long orderId,
    Long userId,
    LocalDateTime completedAt
) implements DomainEvent {

  public static PointPaymentCompletedEvent of(Long orderId, Long userId, LocalDateTime completedAt) {
    return new PointPaymentCompletedEvent(orderId, userId, completedAt);
  }

  @Override
  public String eventType() {
    return EventType.POINT_PAYMENT_COMPLETED.getCode();
  }

  @Override
  public LocalDateTime occurredAt() {
    return completedAt;
  }
}