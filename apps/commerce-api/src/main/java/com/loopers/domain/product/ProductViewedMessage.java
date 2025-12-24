package com.loopers.domain.product;

public record ProductViewedMessage(
        String eventId,
        Long userId,
        Long productId
) {
}
