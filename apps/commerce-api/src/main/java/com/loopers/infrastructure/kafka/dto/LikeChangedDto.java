package com.loopers.infrastructure.kafka.dto;

import java.util.UUID;

public record LikeChangedDto(
        String eventId,
        Long productId,
        String likeType
) {
    public static LikeChangedDto of(Long productId, String likeType) {
        return new LikeChangedDto(
                UUID.randomUUID().toString(),
                productId,
                likeType
        );
    }
}
