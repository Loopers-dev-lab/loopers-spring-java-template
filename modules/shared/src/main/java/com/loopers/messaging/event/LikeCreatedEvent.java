package com.loopers.messaging.event;

import java.time.Instant;

public record LikeCreatedEvent(
        Long productId,
        Instant occurredAt
) {
    public static LikeCreatedEvent from(Long productId) {
        return new LikeCreatedEvent(
                productId,
                Instant.now()
        );
    }
}
