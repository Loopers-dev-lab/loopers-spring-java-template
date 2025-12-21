package com.loopers.domain.metrics;

public interface ProductMetricsRepository {

  void upsertLikeCount(Long productId, int delta, Long occurredAt);

  void upsertSalesCount(Long productId, int quantity, Long occurredAt);

  void upsertViewCount(Long productId, int count, Long occurredAt);
}
