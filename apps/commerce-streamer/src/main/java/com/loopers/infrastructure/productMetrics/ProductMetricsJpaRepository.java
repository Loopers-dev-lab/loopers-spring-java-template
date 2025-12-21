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
    
    Optional<ProductMetrics> findByProductIdAndBucketTime(Long productId, LocalDateTime bucketTime);
    
    List<ProductMetrics> findByProductIdAndBucketTimeBetween(Long productId, LocalDateTime startTime, LocalDateTime endTime);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.likeCount = pm.likeCount + 1 WHERE pm.productId = :productId AND pm.bucketTime = :bucketTime")
    int incrementLikeCount(Long productId, LocalDateTime bucketTime);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.likeCount = CASE WHEN pm.likeCount > 0 THEN pm.likeCount - 1 ELSE 0 END WHERE pm.productId = :productId AND pm.bucketTime = :bucketTime")
    int decrementLikeCount(Long productId, LocalDateTime bucketTime);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.salesCount = pm.salesCount + :quantity WHERE pm.productId = :productId AND pm.bucketTime = :bucketTime")
    int incrementSalesCount(Long productId, LocalDateTime bucketTime, Long quantity);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.viewCount = pm.viewCount + 1 WHERE pm.productId = :productId AND pm.bucketTime = :bucketTime")
    int incrementViewCount(Long productId, LocalDateTime bucketTime);
}
