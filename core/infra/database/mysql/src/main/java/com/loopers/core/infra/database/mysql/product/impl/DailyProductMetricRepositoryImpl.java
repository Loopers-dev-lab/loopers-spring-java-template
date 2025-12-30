package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.repository.DailyProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductMetricAggregation;
import com.loopers.core.infra.database.mysql.product.DailyProductMetricJpaRepository;
import com.loopers.core.infra.database.mysql.product.entity.DailyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DailyProductMetricRepositoryImpl implements DailyProductMetricRepository {

    private final DailyProductMetricJpaRepository repository;

    @Override
    public Optional<DailyProductMetric> findByWithLock(ProductId productId, CreatedAt createdAt) {
        return repository.findByProductIdWithLock(Long.parseLong(Objects.requireNonNull(productId.value())), createdAt.value())
                .map(DailyProductMetricEntity::to);
    }

    @Override
    public DailyProductMetric save(DailyProductMetric dailyProductMetric) {
        return repository.save(DailyProductMetricEntity.from(dailyProductMetric)).to();
    }

    @Override
    public Long countDistinctProductIdsBy(LocalDate startDate, LocalDate endDate) {
        return repository.countDistinctProductIds(startDate, endDate);
    }

    @Override
    public List<ProductMetricAggregation> findAggregatedBy(LocalDate startDate, LocalDate endDate, long partitionOffset, long partitionLimit) {
        return repository.findAggregatedBy(startDate, endDate, partitionOffset, partitionLimit);
    }

    @Override
    public List<DailyProductMetric> saveAll(List<DailyProductMetric> dailyProductMetrics) {
        return repository.saveAll(dailyProductMetrics.stream()
                        .map(DailyProductMetricEntity::from)
                        .toList()
                ).stream()
                .map(DailyProductMetricEntity::to)
                .toList();
    }
}
