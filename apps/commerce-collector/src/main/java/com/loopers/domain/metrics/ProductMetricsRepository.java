package com.loopers.domain.metrics;

import java.util.Optional;

public interface ProductMetricsRepository {
    Optional<ProductMetrics> findByProductId(Long productId);
    Optional<ProductMetrics> findByProductIdWithLock(Long productId);
    ProductMetrics save(ProductMetrics productMetrics);
}
