package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ProductViewedEvent(
    String eventId,
    Long productId,
    LocalDateTime viewedAt
) implements DomainEvent {

  public static ProductViewedEvent of(Long productId, LocalDateTime viewedAt) {
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(viewedAt, "viewedAt은 null일 수 없습니다.");
    return new ProductViewedEvent(UUID.randomUUID().toString(), productId, viewedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_VIEWED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return viewedAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(productId);
  }
}