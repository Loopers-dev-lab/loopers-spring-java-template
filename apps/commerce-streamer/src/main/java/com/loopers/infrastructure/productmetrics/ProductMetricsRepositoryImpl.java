package com.loopers.infrastructure.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {
    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId);
    }

    @Override
    public void upsertLikeCount(Long productId, LocalDate metricDate) {
        productMetricsJpaRepository.upsertLikeCount(productId, metricDate);
    }

    @Override
    public void upsertUnlikeCount(Long productId, LocalDate metricDate) {
        productMetricsJpaRepository.upsertUnlikeCount(productId, metricDate);
    }

    @Override
    public void upsertSalesCount(Long productId, int quantity, BigDecimal amount, LocalDate metricDate) {
        productMetricsJpaRepository.upsertSalesCount(productId, quantity, amount, metricDate);
    }

    @Override
    public void upsertViewCount(Long productId, LocalDate metricDate) {
        productMetricsJpaRepository.upsertViewCount(productId, metricDate);
    }
}
