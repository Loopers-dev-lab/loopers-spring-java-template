package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.WeeklyProductMetricEntity;

import java.util.List;

public interface WeeklyProductMetricBulkRepository {

    void bulkUpsert(List<WeeklyProductMetricEntity> weeklyProductMetrics);
}
