package com.loopers.infrastructure.kafka.dto;

import java.util.UUID;

public record StockChangedDto(
        String eventId,
        Long productId,
        int stock,
        String changedType
) {
    public static StockChangedDto of(Long productId, int stock, String changedType) {
        return new StockChangedDto(
                UUID.randomUUID().toString(),
                productId,
                stock,
                changedType
        );
    }
}
