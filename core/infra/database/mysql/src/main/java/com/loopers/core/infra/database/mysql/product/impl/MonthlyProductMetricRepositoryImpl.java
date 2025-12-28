package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.MonthlyProductMetric;
import com.loopers.core.domain.product.repository.MonthlyProductMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MonthlyProductMetricRepositoryImpl implements MonthlyProductMetricRepository {

    @Override
    public void bulkUpsert(List<MonthlyProductMetric> metrics) {

    }
}
