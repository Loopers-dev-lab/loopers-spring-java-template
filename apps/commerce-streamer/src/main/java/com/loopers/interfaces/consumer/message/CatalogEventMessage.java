package com.loopers.interfaces.consumer.message;

import java.time.ZonedDateTime;

public record CatalogEventMessage(
        String eventId,
        String eventType,
        Long productId,
        String userId,
        long delta,
        ZonedDateTime occurredAt
) {
}
