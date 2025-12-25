package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public record ProductViewedEvent(
        Long productId,
        LocalDateTime viewedAt
) {
    public static ProductViewedEvent of(
            Long productId
    ) {
        return new ProductViewedEvent(productId, LocalDateTime.now());
    }
}
