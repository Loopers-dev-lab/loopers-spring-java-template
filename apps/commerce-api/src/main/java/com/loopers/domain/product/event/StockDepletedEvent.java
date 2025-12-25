package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public class StockDepletedEvent {

    private final Long productId;
    private final LocalDateTime occurredAt;

    private StockDepletedEvent(Long productId) {
        this.productId = productId;
        this.occurredAt = LocalDateTime.now();
    }

    public static StockDepletedEvent of(Long productId) {
        return new StockDepletedEvent(productId);
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
