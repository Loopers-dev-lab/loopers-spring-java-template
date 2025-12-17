package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * ProductMetrics Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository jpaRepository;

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return jpaRepository.findByProductId(productId);
    }

    @Override
    public ProductMetrics save(ProductMetrics productMetrics) {
        return jpaRepository.save(productMetrics);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
