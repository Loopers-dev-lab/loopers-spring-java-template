package com.loopers.core.infra.event.kafka.product.publisher.dto;

import com.loopers.core.domain.product.event.ProductLikeEvent;

public record ProductDetailViewKafkaEvent(String eventId, String productId) {

    public static ProductDetailViewKafkaEvent from(ProductLikeEvent event) {
        return new ProductDetailViewKafkaEvent(event.eventId().value(), event.productId().value());
    }
}
