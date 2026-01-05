package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductEvent;
import com.loopers.domain.product.ProductEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductCoreEventPublisher implements ProductEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
//    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Override
    public void publish(final ProductEvent.ProductViewed event) {
        applicationEventPublisher.publishEvent(event);
    }
}
