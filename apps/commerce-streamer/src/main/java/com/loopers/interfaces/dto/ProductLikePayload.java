package com.loopers.interfaces.dto;

public record ProductLikePayload(
        String eventId,
        Long productId,
        String likeType
) {
}
