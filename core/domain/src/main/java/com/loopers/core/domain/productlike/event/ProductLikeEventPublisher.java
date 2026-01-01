package com.loopers.core.domain.productlike.event;

import com.loopers.core.domain.product.event.ProductLikeEvent;

public interface ProductLikeEventPublisher {

    void publish(ProductLikeEvent event);
}
