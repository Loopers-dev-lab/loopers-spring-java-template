package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.WeeklyProductMetric;
import com.loopers.core.domain.product.repository.WeeklyProductMetricRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WeeklyProductMetricRepositoryImpl implements WeeklyProductMetricRepository {

    @Override
    public void bulkUpsert(List<WeeklyProductMetric> weeklyProductMetrics) {

    }
}
