package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.MonthlyProductMetric;
import com.loopers.core.domain.product.vo.ProductRankings;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyProductMetricRepository {

    void bulkUpsert(List<MonthlyProductMetric> metrics);

    ProductRankings findRankingsBy(
            LocalDate date,
            Integer pageNo,
            Integer pageSize,
            Double payWeight,
            Double viewWeight,
            Double likeWeight
    );
}
