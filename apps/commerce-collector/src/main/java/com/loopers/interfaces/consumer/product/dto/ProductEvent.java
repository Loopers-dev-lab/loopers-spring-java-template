package com.loopers.interfaces.consumer.product.dto;

public record ProductEvent(
        String eventId,
        String eventType,
        Long productId
) {
}
