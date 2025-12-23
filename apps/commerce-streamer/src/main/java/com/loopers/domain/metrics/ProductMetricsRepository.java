package com.loopers.domain.metrics;

import java.util.Optional;

public interface ProductMetricsRepository {
    void saveProductMetrics(ProductMetrics productMetrics);

    Optional<ProductMetrics> findByProductId(Long productId);

    int incrementLikeCount(Long productId);
}

