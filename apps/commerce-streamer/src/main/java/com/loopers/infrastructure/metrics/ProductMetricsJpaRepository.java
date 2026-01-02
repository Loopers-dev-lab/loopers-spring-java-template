package com.loopers.infrastructure.metrics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.loopers.domain.metrics.ProductMetricsAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsId;

/**
 * 상품 메트릭 JPA Repository
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetricsEntity, ProductMetricsId> {

    /**
     * 상품 ID와 날짜로 메트릭 조회
     */
    @Query("SELECT m FROM ProductMetricsEntity m WHERE m.id.productId = :productId AND m.id.metricDate = :metricDate")
    Optional<ProductMetricsEntity> findByProductIdAndMetricDate(
            @Param("productId") Long productId,
            @Param("metricDate") LocalDate metricDate);

    /**
     * 기간별 메트릭 조회
     */
    @Query("SELECT m FROM ProductMetricsEntity m WHERE m.id.metricDate BETWEEN :startDate AND :endDate")
    List<ProductMetricsEntity> findByMetricDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 날짜의 전체 메트릭 조회
     */
    @Query("SELECT m FROM ProductMetricsEntity m WHERE m.id.metricDate = :metricDate")
    List<ProductMetricsEntity> findByMetricDate(@Param("metricDate") LocalDate metricDate);

    /**
     * 기간별 상품 집계 (GROUP BY)
     */
    @Query("""
            SELECT new com.loopers.domain.metrics.ProductMetricsAggregation(
                   m.id.productId,
                   SUM(m.viewCount),
                   SUM(m.likeCount),
                   SUM(m.salesCount),
                   SUM(m.orderCount),
                   SUM(m.totalSalesAmount))
            FROM ProductMetricsEntity m
            WHERE m.id.metricDate BETWEEN :startDate AND :endDate
            GROUP BY m.id.productId
            """)
    List<ProductMetricsAggregation> aggregateByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
