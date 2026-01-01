package com.loopers.core.infra.event.kafka.productlike.publisher.impl;

import com.loopers.core.domain.product.event.ProductLikeEvent;
import com.loopers.core.domain.productlike.event.ProductLikeEventPublisher;
import com.loopers.core.infra.event.kafka.productlike.publisher.dto.ProductLikeKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductLikeEventPublisherImpl implements ProductLikeEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.product-like}")
    private String topic;

    @Override
    public void publish(ProductLikeEvent event) {
        kafkaTemplate.send(topic, event.productId().value(), ProductLikeKafkaEvent.from(event));
    }
}
