package com.loopers.application.order.event;

import java.time.Instant;

public record OrderPaidEvent(
        Long orderId,
        Instant occurredAt
) {
    public static OrderPaidEvent from(Long orderId) {
        return new OrderPaidEvent(
                orderId,
                Instant.now()
        );
    }
}
