package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsDaily;
import com.loopers.domain.metrics.dto.MonthlyAggregationDto;
import com.loopers.domain.metrics.dto.WeeklyAggregationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<ProductMetricsDaily> findAllByMetricDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 주간 집계 쿼리 (DB에서 GROUP BY 수행)
     */
    @Query("""
        SELECT new com.loopers.domain.metrics.dto.WeeklyAggregationDto(
            p.productId,
            :year,
            :week,
            :startDate,
            :endDate,
            SUM(p.likeDelta),
            SUM(p.viewDelta),
            SUM(p.orderDelta),
            0L
        )
        FROM ProductMetricsDaily p
        WHERE p.metricDate BETWEEN :startDate AND :endDate
        GROUP BY p.productId
        ORDER BY p.productId
    """)
    Page<WeeklyAggregationDto> findWeeklyAggregation(
            @Param("year") Integer year,
            @Param("week") Integer week,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    /**
     * 월간 집계 쿼리 (DB에서 GROUP BY 수행)
     */
    @Query("""
        SELECT new com.loopers.domain.metrics.dto.MonthlyAggregationDto(
            p.productId,
            :year,
            :month,
            :startDate,
            :endDate,
            SUM(p.likeDelta),
            SUM(p.viewDelta),
            SUM(p.orderDelta),
            0L
        )
        FROM ProductMetricsDaily p
        WHERE p.metricDate BETWEEN :startDate AND :endDate
        GROUP BY p.productId
        ORDER BY p.productId
    """)
    Page<MonthlyAggregationDto> findMonthlyAggregation(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
