package com.loopers.domain.metrics;

public interface ProductMetricsRepository {

  void upsertLikeCount(Long productId, Integer metricDate, int delta, Long occurredAt);

  void upsertSalesCount(Long productId, Integer metricDate, int quantity, Long occurredAt);

  void upsertViewCount(Long productId, Integer metricDate, int count, Long occurredAt);
}
