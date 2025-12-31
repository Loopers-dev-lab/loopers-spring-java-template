package com.loopers.messaging.event;

import java.time.Instant;

public record ProductViewedEvent(
        Long productId,
        Instant occurredAt
) {
    public static ProductViewedEvent from(Long productId) {
        return new ProductViewedEvent(
                productId,
                Instant.now()
        );
    }
}
