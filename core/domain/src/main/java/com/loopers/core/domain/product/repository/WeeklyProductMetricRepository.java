package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.WeeklyProductMetric;
import com.loopers.core.domain.product.vo.ProductRankings;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyProductMetricRepository {

    void bulkUpsert(List<WeeklyProductMetric> weeklyProductMetrics);

    ProductRankings findRankingsBy(
            LocalDate date,
            Integer pageNo,
            Integer pageSize,
            Double payWeight,
            Double viewWeight,
            Double likeWeight
    );
}
