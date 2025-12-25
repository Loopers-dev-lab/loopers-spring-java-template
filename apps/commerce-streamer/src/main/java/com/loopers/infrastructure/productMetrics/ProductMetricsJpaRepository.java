package com.loopers.infrastructure.productMetrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

  Optional<ProductMetrics> findByProductId(Long productId);

  Optional<ProductMetrics> findByProductIdAndBucketTimeKey(Long productId, String bucketTimeKey);

  @Modifying
  @Query("UPDATE ProductMetrics pm SET pm.viewCount = pm.viewCount + :delta WHERE pm.productId = :productId AND pm.bucketTimeKey = :bucketTimeKey")
  int incrementViewCountByDelta(Long productId, String bucketTimeKey, Long delta);

  @Modifying
  @Query("UPDATE ProductMetrics pm SET pm.likeCount = pm.likeCount + :delta WHERE pm.productId = :productId AND pm.bucketTimeKey = :bucketTimeKey")
  int incrementLikeCountByDelta(Long productId, String bucketTimeKey, Long delta);

  @Modifying
  @Query("UPDATE ProductMetrics pm SET pm.salesRevenue = pm.salesRevenue + :delta WHERE pm.productId = :productId AND pm.bucketTimeKey = :bucketTimeKey")
  int incrementSalesRevenueByDelta(Long productId, String bucketTimeKey, Long delta);
}
