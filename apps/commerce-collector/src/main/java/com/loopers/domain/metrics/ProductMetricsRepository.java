package com.loopers.domain.metrics;

import com.loopers.application.order.OrderMetrics;

import java.util.Map;
import java.util.Optional;

public interface ProductMetricsRepository {
    Optional<ProductMetrics> findByProductId(Long productId);
    Optional<ProductMetrics> findByProductIdWithLock(Long productId);
    ProductMetrics save(ProductMetrics productMetrics);

    // 배치 업데이트 (UPSERT)
    void upsertLikeDeltas(Map<Long, Integer> likeDeltas);
    void upsertViewDeltas(Map<Long, Integer> viewDeltas);
    void upsertOrderDeltas(Map<Long, OrderMetrics> orderMetrics);
}
