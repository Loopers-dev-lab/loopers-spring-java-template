package com.loopers.core.infra.database.mysql.product.impl;

import com.loopers.core.domain.product.ProductMetric;
import com.loopers.core.domain.product.repository.ProductMetricRepository;
import com.loopers.core.infra.database.mysql.product.ProductMetricJpaRepository;
import com.loopers.core.infra.database.mysql.product.entity.ProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductMetricRepositoryImpl implements ProductMetricRepository {

    private final ProductMetricJpaRepository repository;

    @Override
    public ProductMetric save(ProductMetric ProductMetric) {
        return repository.save(ProductMetricEntity.from(ProductMetric)).to();
    }
}
