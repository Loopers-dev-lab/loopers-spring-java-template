package com.loopers.domain;


public interface ProductMetricRepository {
    ProductMetric findByProductId(Long productId);

    ProductMetric save(ProductMetric of);
}
