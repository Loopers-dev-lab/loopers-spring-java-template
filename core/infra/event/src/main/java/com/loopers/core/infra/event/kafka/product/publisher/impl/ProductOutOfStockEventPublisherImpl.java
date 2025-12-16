package com.loopers.core.infra.event.kafka.product.publisher.impl;

import com.loopers.core.domain.product.event.ProductOutOfStockEvent;
import com.loopers.core.domain.product.event.ProductOutOfStockEventPublisher;
import com.loopers.core.infra.event.kafka.product.publisher.dto.ProductOutOfStockKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductOutOfStockEventPublisherImpl implements ProductOutOfStockEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.product-out-of-stock}")
    private String topic;


    @Override
    public void publish(ProductOutOfStockEvent event) {
        kafkaTemplate.send(topic, event.productId().value(), ProductOutOfStockKafkaEvent.from(event));
    }
}
