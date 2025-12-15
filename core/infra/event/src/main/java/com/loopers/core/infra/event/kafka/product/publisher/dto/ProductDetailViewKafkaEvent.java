package com.loopers.core.infra.event.kafka.product.publisher.dto;

import com.loopers.core.domain.product.event.ProductDetailViewEvent;

public record ProductDetailViewKafkaEvent(String eventId, String productId) {

    public static ProductDetailViewKafkaEvent from(ProductDetailViewEvent event) {
        return new ProductDetailViewKafkaEvent(event.eventId().value(), event.productId().value());
    }
}
