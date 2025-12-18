package com.loopers.domain.order.event;

import com.loopers.domain.common.event.ImmediatePublishEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record OrderCreatedEvent(
    String eventId,
    Long orderId,
    Long userId,
    Long couponId,
    Long pointAmount,
    Long totalAmount,
    Long pgAmount,
    LocalDateTime orderedAt
) implements ImmediatePublishEvent {

  public static OrderCreatedEvent of(
      Long orderId,
      Long userId,
      Long couponId,
      Long pointAmount,
      Long totalAmount,
      Long pgAmount,
      LocalDateTime orderedAt
  ) {
    Objects.requireNonNull(orderId, "orderId는 null일 수 없습니다.");
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(orderedAt, "orderedAt는 null일 수 없습니다.");
    return new OrderCreatedEvent(UUID.randomUUID().toString(), orderId, userId, couponId, pointAmount, totalAmount, pgAmount, orderedAt);
  }

  public boolean hasCoupon() {
    return couponId != null;
  }

  public boolean hasPointUsage() {
    return pointAmount != null && pointAmount > 0;
  }

  @Override
  public EventType eventType() {
    return EventType.ORDER_CREATED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return orderedAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(orderId);
  }
}
