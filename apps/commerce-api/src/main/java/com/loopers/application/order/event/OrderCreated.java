package com.loopers.application.order.event;

import java.time.Instant;

public record OrderCreated(
        Long orderId,
        Long userId,
        Instant occurredAt
) {
    public static OrderCreated of(Long orderId, Long userId) {
        return new OrderCreated(
                orderId,
                userId,
                Instant.now()
        );
    }
}
