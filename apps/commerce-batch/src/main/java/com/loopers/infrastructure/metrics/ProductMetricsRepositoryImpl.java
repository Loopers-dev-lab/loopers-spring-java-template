package com.loopers.infrastructure.metrics;

import com.loopers.batch.domain.metrics.ProductMetricsRepository;
import com.loopers.dto.ProductMetricsSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository jpaRepository;

    @Override
    public List<ProductMetricsSummary> findAllByDateRange(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findAllByDateRange(startDate, endDate);
    }
}
