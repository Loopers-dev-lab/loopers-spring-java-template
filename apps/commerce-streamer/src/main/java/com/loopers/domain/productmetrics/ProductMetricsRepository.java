package com.loopers.domain.productmetrics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ProductMetricsRepository {

    Optional<ProductMetrics> findByProductId(Long productId);

    void upsertLikeCount(Long productId, LocalDate metricDate);

    void upsertUnlikeCount(Long productId, LocalDate metricDate);

    void upsertSalesCount(Long productId, int quantity, BigDecimal amount, LocalDate metricDate);

    void upsertViewCount(Long productId, LocalDate metricDate);
}
