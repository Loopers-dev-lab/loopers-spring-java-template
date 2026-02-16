package com.loopers.domain.metrics;

import java.util.List;

public interface ProductMetricsWeeklyRepository {

    ProductMetricsWeekly save(ProductMetricsWeekly metrics);
    void saveAll(List<ProductMetricsWeekly> metricsList);
    int deleteByYearAndWeekBefore(Integer year, Integer week);
    List<ProductMetricsWeekly> findAll();
}
