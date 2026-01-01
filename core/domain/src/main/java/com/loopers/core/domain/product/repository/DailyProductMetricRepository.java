package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductMetricAggregation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyProductMetricRepository {

    Optional<DailyProductMetric> findByWithLock(ProductId productId, CreatedAt createdAt);

    DailyProductMetric save(DailyProductMetric dailyProductMetric);

    Long countDistinctProductIdsBy(LocalDate startDate, LocalDate endDate);

    List<ProductMetricAggregation> findAggregatedBy(LocalDate startDate, LocalDate endDate, long partitionOffset, long partitionLimit);

    List<DailyProductMetric> saveAll(List<DailyProductMetric> dailyProductMetrics);
}
