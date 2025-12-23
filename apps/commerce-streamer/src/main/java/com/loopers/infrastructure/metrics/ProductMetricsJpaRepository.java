package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {
    Optional<ProductMetrics> findByProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductMetrics m SET m.likeCount = m.likeCount + 1 WHERE m.productId = :productId")
    int incrementLikeCount(@Param("productId") Long productId);
}

