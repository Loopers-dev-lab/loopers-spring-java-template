package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;

public record ProductLikedEvent(
    Long userId,
    Long productId,
    LocalDateTime likedAt
) implements DomainEvent {

  public static ProductLikedEvent of(Long userId, Long productId, LocalDateTime likedAt) {
    return new ProductLikedEvent(userId, productId, likedAt);
  }

  @Override
  public String eventType() {
    return EventType.PRODUCT_LIKED.getCode();
  }

  @Override
  public LocalDateTime occurredAt() {
    return likedAt;
  }
}
