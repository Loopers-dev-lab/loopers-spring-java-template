package com.loopers.application.order.event;

import java.time.Instant;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Instant occuredAt
) {
    public static OrderCreatedEvent of(Long orderId, Long userId) {
        return new OrderCreatedEvent(
                orderId,
                userId,
                Instant.now()
        );
    }
}
