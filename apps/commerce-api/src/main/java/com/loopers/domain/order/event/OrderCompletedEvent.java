package com.loopers.domain.order.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;

public record OrderCompletedEvent(
    Long orderId,
    Long userId,
    LocalDateTime completedAt
) implements DomainEvent {

  public static OrderCompletedEvent of(Long orderId, Long userId, LocalDateTime completedAt) {
    return new OrderCompletedEvent(orderId, userId, completedAt);
  }

  @Override
  public String eventType() {
    return EventType.ORDER_COMPLETED.getCode();
  }

  @Override
  public LocalDateTime occurredAt() {
    return completedAt;
  }
}
