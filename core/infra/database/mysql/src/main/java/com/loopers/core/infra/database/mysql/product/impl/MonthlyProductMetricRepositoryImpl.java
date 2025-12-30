package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.MonthlyProductMetric;
import com.loopers.core.domain.product.repository.MonthlyProductMetricRepository;
import com.loopers.core.infra.database.mysql.product.MonthlyProductMetricBulkRepository;
import com.loopers.core.infra.database.mysql.product.entity.MonthlyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MonthlyProductMetricRepositoryImpl implements MonthlyProductMetricRepository {

    private final MonthlyProductMetricBulkRepository bulkRepository;

    @Override
    public void bulkUpsert(List<MonthlyProductMetric> monthlyProductMetrics) {
        bulkRepository.bulkUpsert(monthlyProductMetrics.stream()
                .map(MonthlyProductMetricEntity::from)
                .toList());
    }
}
