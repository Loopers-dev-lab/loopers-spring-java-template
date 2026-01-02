package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsEntity;
import com.loopers.domain.metrics.ProductMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * ProductMetrics JPA Repository
 * - 배치 Job에서 사용하는 집계 쿼리 포함
 */
public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetricsEntity, ProductMetricsId> {

    /**
     * 기간별 상품 집계 (GROUP BY product_id)
     * - 배치 Job에서 사용하는 핵심 쿼리
     *
     * @param startDate 시작 날짜 (포함)
     * @param endDate 종료 날짜 (포함)
     * @return 집계 결과 [productId, viewCount, likeCount, salesCount, orderCount]
     */
    @Query("""
        SELECT m.id.productId, 
               SUM(m.viewCount), 
               SUM(m.likeCount), 
               SUM(m.salesCount), 
               SUM(m.orderCount)
        FROM ProductMetricsEntity m 
        WHERE m.id.metricDate BETWEEN :startDate AND :endDate
        GROUP BY m.id.productId
        """)
    List<Object[]> aggregateByDateRange(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
}