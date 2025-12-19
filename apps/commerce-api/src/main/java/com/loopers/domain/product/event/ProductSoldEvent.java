package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ProductSoldEvent(
    String eventId,
    Long productId,
    Long orderId,
    int quantity,
    LocalDateTime soldAt
) implements DomainEvent {

  public static ProductSoldEvent of(Long productId, Long orderId, int quantity, LocalDateTime soldAt) {
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(orderId, "orderId는 null일 수 없습니다.");
    Objects.requireNonNull(soldAt, "soldAt은 null일 수 없습니다.");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity는 양수여야 합니다: " + quantity);
    }
    return new ProductSoldEvent(UUID.randomUUID().toString(), productId, orderId, quantity, soldAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_SOLD;
  }

  @Override
  public LocalDateTime occurredAt() {
    return soldAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(productId);
  }

  @Override
  public Map<String, Object> payload() {
    return Map.of("quantity", quantity, "orderId", orderId);
  }
}
