package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.MonthlyProductMetric;

import java.util.List;

public interface MonthlyProductMetricRepository {

    void bulkUpsert(List<MonthlyProductMetric> metrics);
}
