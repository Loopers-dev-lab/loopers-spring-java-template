package com.loopers.core.infra.event.kafka.product.publisher.dto;

import com.loopers.core.domain.product.event.ProductOutOfStockEvent;

public record ProductOutOfStockKafkaEvent(
        String eventId,
        String productId
) {
    public static ProductOutOfStockKafkaEvent from(ProductOutOfStockEvent event) {
        return new ProductOutOfStockKafkaEvent(event.eventId().value(), event.productId().value());
    }
}
