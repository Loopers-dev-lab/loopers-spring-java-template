package com.loopers.infrastructure.productMetrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  public Optional<ProductMetrics> findByProductIdAndBucketTimeKey(Long productId, String bucketTimeKey) {
    return productMetricsJpaRepository.findByProductIdAndBucketTimeKey(productId, bucketTimeKey);
  }

  @Override
  public ProductMetrics save(ProductMetrics metrics) {
    return productMetricsJpaRepository.save(metrics);
  }


  @Override
  public int incrementViewCountByDelta(Long productId, String bucketTimeKey, Long delta) {
    return productMetricsJpaRepository.incrementViewCountByDelta(productId, bucketTimeKey, delta);
  }

  @Override
  public int incrementLikeCountByDelta(Long productId, String bucketTimeKey, Long delta) {
    return productMetricsJpaRepository.incrementLikeCountByDelta(productId, bucketTimeKey, delta);
  }

  @Override
  public int incrementSalesRevenueByDelta(Long productId, String bucketTimeKey, Long delta) {
    return productMetricsJpaRepository.incrementSalesRevenueByDelta(productId, bucketTimeKey, delta);
  }
}
