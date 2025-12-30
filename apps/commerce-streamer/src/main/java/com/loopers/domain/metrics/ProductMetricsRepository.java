package com.loopers.domain.metrics;

import java.util.Optional;

public interface ProductMetricsRepository {

    Optional<ProductMetrics> findByProductId(Long productId);

    ProductMetrics save(ProductMetrics metrics);
}
