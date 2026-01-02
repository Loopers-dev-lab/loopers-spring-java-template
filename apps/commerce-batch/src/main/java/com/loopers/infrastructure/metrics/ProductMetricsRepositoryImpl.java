package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsId;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProductMetrics Repository 구현체
 * - JPA Repository를 래핑하여 도메인 인터페이스 구현
 */
@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository jpaRepository;

    @Override
    public ProductMetricsEntity save(ProductMetricsEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<ProductMetricsEntity> findById(ProductMetricsId id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ProductMetricsEntity> findByProductIdAndMetricDate(Long productId, LocalDate metricDate) {
        ProductMetricsId id = ProductMetricsId.of(productId, metricDate);
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProductMetricsEntity> findByMetricDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByMetricDateBetween(startDate, endDate);
    }

    @Override
    public List<Object[]> aggregateByDateRange(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.aggregateByDateRange(startDate, endDate);
    }
}
