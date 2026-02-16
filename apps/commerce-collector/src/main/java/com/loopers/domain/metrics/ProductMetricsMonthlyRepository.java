package com.loopers.domain.metrics;

import java.util.List;

public interface ProductMetricsMonthlyRepository {
    ProductMetricsMonthly save(ProductMetricsMonthly metrics);
    void saveAll(List<ProductMetricsMonthly> metricsList);
    int deleteByYearAndMonthBefore(Integer year, Integer month);
    List<ProductMetricsMonthly> findAll();
}
