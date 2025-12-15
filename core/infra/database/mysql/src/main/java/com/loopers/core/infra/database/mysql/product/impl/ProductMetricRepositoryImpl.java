package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.ProductMetric;
import com.loopers.core.domain.product.repository.ProductMetricRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.infra.database.mysql.product.ProductMetricJpaRepository;
import com.loopers.core.infra.database.mysql.product.entity.ProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductMetricRepositoryImpl implements ProductMetricRepository {

    private final ProductMetricJpaRepository repository;

    @Override
    public Optional<ProductMetric> findByWithLock(ProductId productId) {
        return repository.findByProductIdWithLock(Long.parseLong(Objects.requireNonNull(productId.value())))
                .map(ProductMetricEntity::to);
    }

    @Override
    public ProductMetric save(ProductMetric ProductMetric) {
        return repository.save(ProductMetricEntity.from(ProductMetric)).to();
    }
}
