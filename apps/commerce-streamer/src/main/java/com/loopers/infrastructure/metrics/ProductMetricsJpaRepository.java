package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, ProductMetricsId> {

    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.id.productId = :productId AND pm.id.metricsDate = :date")
    Optional<ProductMetrics> findByProductIdAndDate(@Param("productId") Long productId, @Param("date") LocalDate date);
}
