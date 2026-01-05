package com.loopers.domain.product;

public interface ProductEventPublisher {
    void publish(ProductEvent.ProductViewed event);
}
