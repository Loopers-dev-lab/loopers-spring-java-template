package com.loopers.domain.product;

public interface ProductOutboxEventRepository {
    ProductOutboxEvent save(ProductOutboxEvent productOutboxEvent);
}
