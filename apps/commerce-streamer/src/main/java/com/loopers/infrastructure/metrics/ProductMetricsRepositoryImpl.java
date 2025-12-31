package com.loopers.infrastructure.metrics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsId;
import com.loopers.domain.metrics.ProductMetricsRepository;

import lombok.RequiredArgsConstructor;

/**
 * 상품 메트릭 Repository 구현체
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
    public Optional<ProductMetricsEntity> findById(ProductMetricsId id) {
        return productMetricsJpaRepository.findById(id);
    }

    @Override
    public Optional<ProductMetricsEntity> findByProductIdAndMetricDate(Long productId, LocalDate metricDate) {
        return productMetricsJpaRepository.findByProductIdAndMetricDate(productId, metricDate);
    }

    @Override
    public List<ProductMetricsEntity> findByMetricDateBetween(LocalDate startDate, LocalDate endDate) {
        return productMetricsJpaRepository.findByMetricDateBetween(startDate, endDate);
    }

    @Override
    public List<ProductMetricsEntity> findByMetricDate(LocalDate metricDate) {
        return productMetricsJpaRepository.findByMetricDate(metricDate);
    }

    @Override
    public List<Object[]> aggregateByDateRange(LocalDate startDate, LocalDate endDate) {
        return productMetricsJpaRepository.aggregateByDateRange(startDate, endDate);
    }
}
