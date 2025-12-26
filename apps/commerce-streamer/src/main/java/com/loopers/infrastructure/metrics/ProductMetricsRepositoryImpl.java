package com.loopers.infrastructure.metrics;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsRepository;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Component
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {
    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public ProductMetricsEntity save(ProductMetricsEntity metrics) {
        return productMetricsJpaRepository.save(metrics);
    }

    @Override
    public Optional<ProductMetricsEntity> findById(Long productId) {
        return productMetricsJpaRepository.findById(productId);
    }

    @Override
    public void deleteAll() {
        productMetricsJpaRepository.deleteAll();
    }
}
