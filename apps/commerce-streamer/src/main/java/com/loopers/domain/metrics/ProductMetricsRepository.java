package com.loopers.domain.metrics;

import java.util.Optional;

/**
 * ProductMetrics Repository
 */
public interface ProductMetricsRepository {

    /**
     * productId로 조회
     */
    Optional<ProductMetrics> findByProductId(Long productId);

    /**
     * ProductMetrics 저장
     */
    ProductMetrics save(ProductMetrics productMetrics);

    /**
     * 모든 ProductMetrics 삭제 (테스트용)
     */
    void deleteAll();
}
