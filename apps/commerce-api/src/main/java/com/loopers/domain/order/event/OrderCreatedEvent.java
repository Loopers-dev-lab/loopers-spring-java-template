package com.loopers.domain.order.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;


public record OrderCreatedEvent(
    Long orderId,
    Long userId,
    Long couponId,
    Long pointAmount,
    Long totalAmount,
    Long pgAmount,
    LocalDateTime orderedAt
) implements DomainEvent {

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
    return new OrderCreatedEvent(orderId, userId, couponId, pointAmount, totalAmount, pgAmount, orderedAt);
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
}
