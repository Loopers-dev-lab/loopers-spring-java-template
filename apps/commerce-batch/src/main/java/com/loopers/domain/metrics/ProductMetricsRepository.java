package com.loopers.domain.metrics;

import com.loopers.domain.ranking.ProductMetricsWeekly;
import com.loopers.domain.ranking.ProductMetricsMonthly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {

    @Query(value = """
        SELECT 
            pm.product_id as productId,
            COALESCE(SUM(pm.like_count), 0) as likeCount,
            COALESCE(SUM(pm.sales_revenue), 0) as orderCount,
            COALESCE(SUM(pm.view_count), 0) as viewCount,
            :period as yearMonthWeek
        FROM product_metrics pm
        WHERE pm.bucket_time_key >= :startTimeKey
        GROUP BY pm.product_id
        ORDER BY pm.product_id
        """, nativeQuery = true)
    Page<ProductMetricsWeekly> findWeeklyMetrics(@Param("startTimeKey") String startTimeKey, @Param("period") String period, Pageable pageable);

    @Query(value = """
        SELECT 
            pm.product_id as productId,
            COALESCE(SUM(pm.like_count), 0) as likeCount,
            COALESCE(SUM(pm.sales_revenue), 0) as orderCount,
            COALESCE(SUM(pm.view_count), 0) as viewCount,
            :period as yearMonth
        FROM product_metrics pm
        WHERE pm.bucket_time_key >= :startTimeKey
        GROUP BY pm.product_id
        ORDER BY pm.product_id
        """, nativeQuery = true)
    Page<ProductMetricsMonthly> findMonthlyMetrics(@Param("startTimeKey") String startTimeKey, @Param("period") String period, Pageable pageable);
}