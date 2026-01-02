package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsDailyJpaRepository extends JpaRepository<ProductMetricsDaily, Long> {

    Optional<ProductMetricsDaily> findByProductIdAndMetricDate(Long productId, LocalDate metricDate);
    List<ProductMetricsDaily> findAllByMetricDateAndIsProcessed(LocalDate metricDate, boolean isProcessed);

    @Modifying
    @Query("delete from ProductMetricsDaily m where m.metricDate < :cutoffDate")
    int deleteByMetricDateBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
