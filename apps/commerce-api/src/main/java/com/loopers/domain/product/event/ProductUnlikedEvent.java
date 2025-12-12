package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;

public record ProductUnlikedEvent(
    Long userId,
    Long productId,
    LocalDateTime unlikedAt
) implements DomainEvent {

  public static ProductUnlikedEvent of(Long userId, Long productId, LocalDateTime unlikedAt) {
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(unlikedAt, "unlikedAt은 null일 수 없습니다.");
    return new ProductUnlikedEvent(userId, productId, unlikedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_UNLIKED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return unlikedAt;
  }
}
