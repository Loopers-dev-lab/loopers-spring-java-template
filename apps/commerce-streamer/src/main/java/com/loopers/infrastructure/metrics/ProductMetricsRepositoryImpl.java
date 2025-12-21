package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

  private final ProductMetricsJpaRepository jpaRepository;

  @Override
  public void upsertLikeCount(Long productId, int delta, Long occurredAt) {
    jpaRepository.upsertLikeCount(productId, delta, occurredAt);
  }

  @Override
  public void upsertSalesCount(Long productId, int quantity, Long occurredAt) {
    jpaRepository.upsertSalesCount(productId, quantity, occurredAt);
  }

  @Override
  public void upsertViewCount(Long productId, int count, Long occurredAt) {
    jpaRepository.upsertViewCount(productId, count, occurredAt);
  }
}
