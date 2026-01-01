package com.loopers.domain;


public interface ProductMetricRepository {
    ProductMetric findByProductIdAndDate(Long productId, String date);

    ProductMetric save(ProductMetric of);
}
