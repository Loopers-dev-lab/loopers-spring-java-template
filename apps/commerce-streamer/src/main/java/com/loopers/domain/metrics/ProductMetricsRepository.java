package com.loopers.domain.metrics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsRepository {

  Optional<ProductMetrics> findByProductId(Long productId);
  
  Optional<ProductMetrics> findByProductIdAndBucketTime(Long productId, LocalDateTime bucketTime);
  
  List<ProductMetrics> findByProductIdAndBucketTimeBetween(Long productId, LocalDateTime startTime, LocalDateTime endTime);

  ProductMetrics save(ProductMetrics metrics);

  int incrementLikeCount(Long productId, LocalDateTime bucketTime);

  int decrementLikeCount(Long productId, LocalDateTime bucketTime);

  int incrementSalesCount(Long productId, LocalDateTime bucketTime, Long quantity);

  int incrementViewCount(Long productId, LocalDateTime bucketTime);
}
