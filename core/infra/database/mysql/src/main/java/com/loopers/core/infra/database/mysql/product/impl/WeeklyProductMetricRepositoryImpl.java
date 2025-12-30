package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.WeeklyProductMetric;
import com.loopers.core.domain.product.repository.WeeklyProductMetricRepository;
import com.loopers.core.infra.database.mysql.product.WeeklyProductMetricBulkRepository;
import com.loopers.core.infra.database.mysql.product.entity.WeeklyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WeeklyProductMetricRepositoryImpl implements WeeklyProductMetricRepository {

    private final WeeklyProductMetricBulkRepository bulkRepository;

    @Override
    public void bulkUpsert(List<WeeklyProductMetric> weeklyProductMetrics) {
        List<WeeklyProductMetricEntity> entities = weeklyProductMetrics.stream()
                .map(WeeklyProductMetricEntity::from)
                .toList();
        bulkRepository.bulkUpsert(entities);
    }
}
