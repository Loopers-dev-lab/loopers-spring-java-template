package com.loopers.core.domain.product.event;

public interface ProductOutOfStockEventPublisher {

    void publish(ProductOutOfStockEvent event);
}
