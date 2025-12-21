package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ProductUnlikedEvent(
    String eventId,
    Long userId,
    Long productId,
    LocalDateTime unlikedAt
) implements DomainEvent {

  public static ProductUnlikedEvent of(Long userId, Long productId, LocalDateTime unlikedAt) {
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(unlikedAt, "unlikedAt은 null일 수 없습니다.");
    return new ProductUnlikedEvent(UUID.randomUUID().toString(), userId, productId, unlikedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_UNLIKED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return unlikedAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(productId);
  }
}
