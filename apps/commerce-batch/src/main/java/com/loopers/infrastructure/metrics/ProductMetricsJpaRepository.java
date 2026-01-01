package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {
    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.metricsDate = :date ORDER BY pm.productId")
    List<ProductMetrics> findByMetricsDate(@Param("date") LocalDate date);
}


