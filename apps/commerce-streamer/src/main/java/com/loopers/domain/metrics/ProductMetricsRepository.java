package com.loopers.domain.metrics;

import java.util.Optional;

public interface ProductMetricsRepository {

  Optional<ProductMetrics> findByProductId(Long productId);

  ProductMetrics save(ProductMetrics metrics);

  int incrementLikeCount(Long productId);

  int decrementLikeCount(Long productId);

  int incrementSalesCount(Long productId, Long quantity);

  int incrementViewCount(Long productId);
}
