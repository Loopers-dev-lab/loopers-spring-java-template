package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductMetricsRepository {
    void saveProductMetrics(ProductMetrics productMetrics);

    Optional<ProductMetrics> findByProductId(Long productId);

    Optional<ProductMetrics> findByProductIdAndMetricsDate(Long productId, LocalDate metricsDate);

    int incrementLikeCount(Long productId, LocalDate metricsDate);
}

