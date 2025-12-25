package com.loopers.core.domain.product.event;

public interface ProductDetailViewEventPublisher {

    void publish(ProductDetailViewEvent event);
}
