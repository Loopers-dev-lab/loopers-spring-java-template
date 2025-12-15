package com.loopers.application.payment.event;

import java.time.Instant;

public record PaymentFailedEvent(
        Long userId,
        Long orderId,
        String msg,
        Instant occurredAt
) {
    public static PaymentFailedEvent of(Long userId, Long orderId, Exception e) {
        return new PaymentFailedEvent(
                userId,
                orderId,
                e.getMessage(),
                Instant.now()
        );
    }
}
