package com.loopers.infrastructure.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import com.loopers.domain.productmetrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public void upsertLikeCount(Long productId) {
        productMetricsJpaRepository.upsertLikeCount(productId);
    }

    @Override
    public void upsertUnlikeCount(Long productId) {
        productMetricsJpaRepository.upsertUnlikeCount(productId);
    }

    @Override
    public void upsertSalesCount(Long productId, int quantity) {
        productMetricsJpaRepository.upsertSalesCount(productId, quantity);
    }

    @Override
    public void upsertViewCount(Long productId) {
        productMetricsJpaRepository.upsertViewCount(productId);
    }
}
