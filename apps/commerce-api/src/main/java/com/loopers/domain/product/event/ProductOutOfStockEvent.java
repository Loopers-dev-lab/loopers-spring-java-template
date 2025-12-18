package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ProductOutOfStockEvent(
    String eventId,
    Long productId,
    LocalDateTime soldOutAt
) implements DomainEvent {

  public static ProductOutOfStockEvent of(Long productId, LocalDateTime soldOutAt) {
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(soldOutAt, "soldOutAt은 null일 수 없습니다.");
    return new ProductOutOfStockEvent(UUID.randomUUID().toString(), productId, soldOutAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_OUT_OF_STOCK;
  }

  @Override
  public LocalDateTime occurredAt() {
    return soldOutAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(productId);
  }
}