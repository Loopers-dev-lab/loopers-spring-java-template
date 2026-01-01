package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsRepository {
    List<ProductMetrics> findByMetricsDate(LocalDate metricsDate);
}

