package com.loopers.domain.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ProductLikedEvent(
    String eventId,
    Long userId,
    Long productId,
    LocalDateTime likedAt
) implements DomainEvent {

  public static ProductLikedEvent of(Long userId, Long productId, LocalDateTime likedAt) {
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
    Objects.requireNonNull(likedAt, "likedAt은 null일 수 없습니다.");
    return new ProductLikedEvent(UUID.randomUUID().toString(), userId, productId, likedAt);
  }

  @Override
  public EventType eventType() {
    return EventType.PRODUCT_LIKED;
  }

  @Override
  public LocalDateTime occurredAt() {
    return likedAt;
  }

  @Override
  public String aggregateId() {
    return String.valueOf(productId);
  }
}
