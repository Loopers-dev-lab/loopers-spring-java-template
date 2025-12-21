package com.loopers.domain.order.event;

import com.loopers.domain.common.event.ImmediatePublishEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCompletedEvent(
    String eventId,
    Long orderId,
    Long userId,
    LocalDateTime completedAt
) implements ImmediatePublishEvent {

  public static OrderCompletedEvent of(Long orderId, Long userId, LocalDateTime completedAt) {
    return new OrderCompletedEvent(UUID.randomUUID().toString(), orderId, userId, completedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.ORDER_COMPLETED;
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
