package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public void saveProductMetrics(ProductMetrics productMetrics) {
        productMetricsJpaRepository.save(productMetrics);
    }

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId);
    }

    @Override
    public Optional<ProductMetrics> findByProductIdAndMetricsDate(Long productId, LocalDate metricsDate) {
        return productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, metricsDate);
    }

    @Override
    public int incrementLikeCount(Long productId, LocalDate metricsDate) {
        return productMetricsJpaRepository.incrementLikeCount(productId, metricsDate);
    }
}

