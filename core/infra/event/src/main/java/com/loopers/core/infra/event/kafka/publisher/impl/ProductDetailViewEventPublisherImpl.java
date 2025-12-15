package com.loopers.core.infra.event.kafka.publisher.impl;

import com.loopers.core.domain.product.event.ProductDetailViewEvent;
import com.loopers.core.domain.product.event.ProductDetailViewEventPublisher;
import com.loopers.core.infra.event.kafka.publisher.dto.ProductDetailViewKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductDetailViewEventPublisherImpl implements ProductDetailViewEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.product-detail-viewed}")
    private String topic;

    @Override
    public void publish(ProductDetailViewEvent event) {
        kafkaTemplate.send(topic, event.productId().value(), ProductDetailViewKafkaEvent.from(event));
    }
}
