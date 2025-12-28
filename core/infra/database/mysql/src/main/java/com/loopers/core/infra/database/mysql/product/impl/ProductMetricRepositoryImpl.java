package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.repository.ProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.infra.database.mysql.product.ProductMetricJpaRepository;
import com.loopers.core.infra.database.mysql.product.entity.DailyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductMetricRepositoryImpl implements ProductMetricRepository {

    private final ProductMetricJpaRepository repository;

    @Override
    public Optional<DailyProductMetric> findByWithLock(ProductId productId, CreatedAt createdAt) {
        return repository.findByProductIdWithLock(Long.parseLong(Objects.requireNonNull(productId.value())), createdAt.value())
                .map(DailyProductMetricEntity::to);
    }

    @Override
    public DailyProductMetric save(DailyProductMetric dailyProductMetric) {
        return repository.save(DailyProductMetricEntity.from(dailyProductMetric)).to();
    }
}
