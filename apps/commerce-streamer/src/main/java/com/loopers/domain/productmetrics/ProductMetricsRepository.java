package com.loopers.domain.productmetrics;

import java.util.Optional;

public interface ProductMetricsRepository {

    Optional<ProductMetrics> findByProductId(Long productId);

    void upsertLikeCount(Long productId);

    void upsertUnlikeCount(Long productId);

    void upsertSalesCount(Long productId, int quantity);

    void upsertViewCount(Long productId);
}
