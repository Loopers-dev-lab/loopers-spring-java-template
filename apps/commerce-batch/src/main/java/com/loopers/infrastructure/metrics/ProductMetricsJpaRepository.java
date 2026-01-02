package com.loopers.infrastructure.metrics;

import com.loopers.batch.domain.metrics.ProductMetrics;
import com.loopers.batch.domain.metrics.ProductMetricsId;
import com.loopers.dto.ProductMetricsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, ProductMetricsId> {

    @Query("""
        SELECT new com.loopers.dto.ProductMetricsSummary(
            pm.id.productId,
            pm.id.metricsDate,
            CAST(pm.likesDelta AS long),
            CAST(pm.salesDelta AS long),
            CAST(pm.viewsDelta AS long)
        )
        FROM ProductMetrics pm
        WHERE pm.id.metricsDate BETWEEN :startDate AND :endDate
        ORDER BY pm.id.productId, pm.id.metricsDate
        """)
    List<ProductMetricsSummary> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
