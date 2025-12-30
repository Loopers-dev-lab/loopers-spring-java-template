package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.MonthlyProductMetricEntity;

import java.util.List;

public interface MonthlyProductMetricBulkRepository {

    void bulkUpsert(List<MonthlyProductMetricEntity> monthlyProductMetrics);
}
