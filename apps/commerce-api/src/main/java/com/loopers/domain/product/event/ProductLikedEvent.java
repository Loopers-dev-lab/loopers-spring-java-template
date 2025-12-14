package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;

public record ProductLikedEvent(
    Long userId,
    Long productId,
    LocalDateTime likedAt
) implements DomainEvent {

  public static ProductLikedEvent of(Long userId, Long productId, LocalDateTime likedAt) {
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(likedAt, "likedAt은 null일 수 없습니다.");
    return new ProductLikedEvent(userId, productId, likedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_LIKED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return likedAt;
  }
}
