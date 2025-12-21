package com.loopers.infrastructure.productMetrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
  public Optional<ProductMetrics> findByProductIdAndBucketTime(Long productId, LocalDateTime bucketTime) {
    return productMetricsJpaRepository.findByProductIdAndBucketTime(productId, bucketTime);
  }
  
  @Override
  public List<ProductMetrics> findByProductIdAndBucketTimeBetween(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
    return productMetricsJpaRepository.findByProductIdAndBucketTimeBetween(productId, startTime, endTime);
  }

  @Override
  public ProductMetrics save(ProductMetrics metrics) {
    return productMetricsJpaRepository.save(metrics);
  }

  @Override
  public int incrementLikeCount(Long productId, LocalDateTime bucketTime) {
    return productMetricsJpaRepository.incrementLikeCount(productId, bucketTime);
  }

  @Override
  public int decrementLikeCount(Long productId, LocalDateTime bucketTime) {
    return productMetricsJpaRepository.decrementLikeCount(productId, bucketTime);
  }

  @Override
  public int incrementSalesCount(Long productId, LocalDateTime bucketTime, Long quantity) {
    return productMetricsJpaRepository.incrementSalesCount(productId, bucketTime, quantity);
  }

  @Override
  public int incrementViewCount(Long productId, LocalDateTime bucketTime) {
    return productMetricsJpaRepository.incrementViewCount(productId, bucketTime);
  }
}
