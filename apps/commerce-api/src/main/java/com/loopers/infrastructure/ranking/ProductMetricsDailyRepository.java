package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.ProductMetricsDaily;
import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.ProductMetricsMonthly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductMetricsDailyRepository extends JpaRepository<ProductMetricsDaily, Long> {

  /**
   * 주간 기간의 일간 데이터를 집계하여 ProductMetricsWeekly 형태로 조회
   * startDate부터 endDate까지의 데이터를 상품별로 합산하여 정렬
   */
  @Query("""
      SELECT new com.loopers.domain.metrics.ProductMetricsWeekly(
          pmd.productId,
          CAST(SUM(pmd.likeCount) AS INTEGER),
          CAST(SUM(pmd.orderCount) AS INTEGER), 
          CAST(SUM(pmd.viewCount) AS INTEGER),
          :yearMonthWeek
      )
      FROM ProductMetricsDaily pmd 
      WHERE pmd.yearMonthDay BETWEEN :startDate AND :endDate
      GROUP BY pmd.productId
      """)
  Page<ProductMetricsWeekly> findWeeklyAggregateByDateRange(
      @Param("startDate") String startDate,
      @Param("endDate") String endDate,
      @Param("yearMonthWeek") String yearMonthWeek,
      Pageable pageable
  );

  /**
   * 월간 기간의 일간 데이터를 집계하여 ProductMetricsMonthly 형태로 조회
   * yearMonth에 해당하는 모든 일간 데이터를 상품별로 합산하여 정렬
   */
  @Query("""
      SELECT new com.loopers.domain.metrics.ProductMetricsMonthly(
          pmd.productId,
          CAST(SUM(pmd.likeCount) AS INTEGER),
          CAST(SUM(pmd.orderCount) AS INTEGER),
          CAST(SUM(pmd.viewCount) AS INTEGER),
          :yearMonth
      )
      FROM ProductMetricsDaily pmd 
      WHERE pmd.yearMonthDay LIKE :yearMonthPattern
      GROUP BY pmd.productId
      """)
  Page<ProductMetricsMonthly> findMonthlyAggregateByYearMonth(
      @Param("yearMonthPattern") String yearMonthPattern,
      @Param("yearMonth") String yearMonth,
      Pageable pageable
  );

  /**
   * 특정 날짜의 데이터 존재 여부 확인
   */
  boolean existsByYearMonthDay(String yearMonthDay);

  /**
   * 날짜 범위의 데이터 존재 여부 확인
   */
  @Query("SELECT COUNT(DISTINCT pmd.yearMonthDay) FROM ProductMetricsDaily pmd WHERE pmd.yearMonthDay BETWEEN :startDate AND :endDate")
  long countDistinctDaysByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
