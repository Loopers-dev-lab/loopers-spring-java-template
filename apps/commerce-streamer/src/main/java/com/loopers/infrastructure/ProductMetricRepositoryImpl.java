package com.loopers.infrastructure;

import com.loopers.domain.ProductMetric;
import com.loopers.domain.ProductMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ProductMetricRepositoryImpl implements ProductMetricRepository {
    private final ProductMetricJpaRepository productMetricJpaRepository;

    @Override
    public ProductMetric findByProductId(Long productId) {
        return productMetricJpaRepository.findByProductId(productId);
    }

    @Override
    public ProductMetric save(ProductMetric productMetric) {
        return productMetricJpaRepository.save(productMetric);
    }
}
