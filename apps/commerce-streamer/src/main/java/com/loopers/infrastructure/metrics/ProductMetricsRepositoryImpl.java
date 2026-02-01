package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

  private final ProductMetricsJpaRepository jpaRepository;

  @Override
  public void upsertLikeCount(Long productId, Integer metricDate, int delta, Long occurredAt) {
    validateParams(productId, metricDate, occurredAt);
    jpaRepository.upsertLikeCount(productId, metricDate, delta, occurredAt);
  }

  @Override
  public void upsertSalesCount(Long productId, Integer metricDate, int quantity, Long occurredAt) {
    validateParams(productId, metricDate, occurredAt);
    jpaRepository.upsertSalesCount(productId, metricDate, quantity, occurredAt);
  }

  @Override
  public void upsertViewCount(Long productId, Integer metricDate, int count, Long occurredAt) {
    validateParams(productId, metricDate, occurredAt);
    jpaRepository.upsertViewCount(productId, metricDate, count, occurredAt);
  }

  private void validateParams(Long productId, Integer metricDate, Long occurredAt) {
    if (productId == null || metricDate == null || occurredAt == null) {
      throw new IllegalArgumentException("productId, metricDate, occurredAt은 필수입니다");
    }
  }
}
