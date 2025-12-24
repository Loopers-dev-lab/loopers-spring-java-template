package com.loopers.infrastructure.kafka.dto;

import java.util.UUID;

public record ProductViewedDto(
        String eventId,
        Long productId
) {
    public static ProductViewedDto of(Long productId) {
        return new ProductViewedDto(
                UUID.randomUUID().toString(),
                productId
        );
    }
}
