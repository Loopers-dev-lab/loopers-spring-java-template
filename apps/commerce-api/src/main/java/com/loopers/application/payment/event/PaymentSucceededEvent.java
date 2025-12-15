package com.loopers.application.payment.event;

import java.time.Instant;

public record PaymentSucceededEvent(
        Long orderId,
        Instant occurredAt
) {
    public static PaymentSucceededEvent from(Long orderId) {
        return new PaymentSucceededEvent(
                orderId,
                Instant.now()
        );
    }
}
