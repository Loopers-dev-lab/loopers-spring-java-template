package com.loopers.infrastructure.productMetrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.likeCount = pm.likeCount + 1 WHERE pm.productId = :productId")
    int incrementLikeCount(Long productId);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.likeCount = CASE WHEN pm.likeCount > 0 THEN pm.likeCount - 1 ELSE 0 END WHERE pm.productId = :productId")
    int decrementLikeCount(Long productId);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.salesCount = pm.salesCount + :quantity WHERE pm.productId = :productId")
    int incrementSalesCount(Long productId, Long quantity);

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.viewCount = pm.viewCount + 1 WHERE pm.productId = :productId")
    int incrementViewCount(Long productId);
}
