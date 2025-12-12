package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;

public record ProductUnlikedEvent(
    Long userId,
    Long productId,
    LocalDateTime unlikedAt
) implements DomainEvent {

  public static ProductUnlikedEvent of(Long userId, Long productId, LocalDateTime unlikedAt) {
    return new ProductUnlikedEvent(userId, productId, unlikedAt);
  }

  @Override
  public String eventType() {
    return EventType.PRODUCT_UNLIKED.getCode();
  }

  @Override
  public LocalDateTime occurredAt() {
    return unlikedAt;
  }
}
