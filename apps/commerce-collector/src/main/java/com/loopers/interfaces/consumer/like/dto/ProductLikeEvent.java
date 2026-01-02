package com.loopers.interfaces.consumer.like.dto;

public record ProductLikeEvent(
        String eventId,
        String eventType,
        Long productId
) {
}
