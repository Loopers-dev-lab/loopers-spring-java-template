package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductMetricsRepository {

    Optional<ProductMetrics> findByProductIdAndDate(Long productId, LocalDate date);

    ProductMetrics save(ProductMetrics productMetrics);
}
