package com.loopers.batch.domain.metrics;

import com.loopers.dto.ProductMetricsSummary;

import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsRepository {
    List<ProductMetricsSummary> findAllByDateRange(LocalDate startDate, LocalDate endDate);
}
