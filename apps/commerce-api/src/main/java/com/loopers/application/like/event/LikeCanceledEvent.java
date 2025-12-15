package com.loopers.application.like.event;

import java.time.Instant;

public record LikeCanceledEvent(
        Long productId,
        Instant occurredAt
) {
    public static LikeCanceledEvent from(Long productId) {
        return new LikeCanceledEvent(
                productId,
                Instant.now()
        );
    }
}
