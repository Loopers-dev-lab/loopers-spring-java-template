package com.loopers.core.infra.event.kafka.productlike.publisher.dto;

import com.loopers.core.domain.product.event.ProductLikeEvent;

public record ProductLikeKafkaEvent(
        String eventId,
        String productId
) {

    public static ProductLikeKafkaEvent from(ProductLikeEvent event) {
        return new ProductLikeKafkaEvent(event);
    }
}
