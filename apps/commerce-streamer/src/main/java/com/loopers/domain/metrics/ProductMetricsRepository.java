package com.loopers.domain.metrics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductMetricsRepository {

  Optional<ProductMetrics> findByProductId(Long productId);

  Optional<ProductMetrics> findByProductIdAndBucketTimeKey(Long productId, String bucketTimeKey);

  ProductMetrics save(ProductMetrics metrics);

  int incrementViewCountByDelta(Long productId, String bucketTimeKey, Long delta);

  int incrementLikeCountByDelta(Long productId, String bucketTimeKey, Long delta);

  int incrementSalesRevenueByDelta(Long productId, String bucketTimeKey, Long delta);


}
