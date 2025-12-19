package com.loopers.infrastructure.productMetrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {
  private final ProductMetricsJpaRepository productMetricsJpaRepository;

  @Override
  public Optional<ProductMetrics> findByProductId(Long productId) {
    return productMetricsJpaRepository.findByProductId(productId);
  }

  @Override
  public ProductMetrics save(ProductMetrics metrics) {
    return productMetricsJpaRepository.save(metrics);
  }

  @Override
  public int incrementLikeCount(Long productId) {
    return productMetricsJpaRepository.incrementLikeCount(productId);
  }

  @Override
  public int decrementLikeCount(Long productId) {
    return productMetricsJpaRepository.decrementLikeCount(productId);
  }

  @Override
  public int incrementSalesCount(Long productId, Long quantity) {
    return productMetricsJpaRepository.incrementSalesCount(productId, quantity);
  }

  @Override
  public int incrementViewCount(Long productId) {
    return productMetricsJpaRepository.incrementViewCount(productId);
  }
}
