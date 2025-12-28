package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.WeeklyProductMetric;

import java.util.List;

public interface WeeklyProductMetricRepository {

    void bulkUpsert(List<WeeklyProductMetric> weeklyProductMetrics);
}
