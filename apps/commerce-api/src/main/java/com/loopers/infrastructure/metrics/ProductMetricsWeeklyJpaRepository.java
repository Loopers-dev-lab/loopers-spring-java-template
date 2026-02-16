package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsWeekly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductMetricsWeeklyJpaRepository extends JpaRepository<ProductMetricsWeekly, Long> {

    /**
     * 특정 년도/주차의 랭킹 조회 (좋아요 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByTotalLikeCountDesc(int year, int week, Pageable pageable);

    /**
     * 특정 년도/주차의 랭킹 조회 (조회수 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByTotalViewCountDesc(int year, int week, Pageable pageable);

    /**
     * 특정 년도/주차의 랭킹 조회 (주문수 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByTotalOrderCountDesc(int year, int week, Pageable pageable);

    /**
     * 특정 년도/주차의 랭킹 조회 (종합 점수 기준 정렬)
     * 종합 점수 = (like * 0.2) + (view * 0.1) + (order * 0.6)
     */
    @Query("""
        SELECT w FROM ProductMetricsWeekly w
        WHERE w.year = :year AND w.week = :week
        ORDER BY (w.totalLikeCount * 0.2 + w.totalViewCount * 0.1 + w.totalOrderCount * 0.6) DESC
        """)
    List<ProductMetricsWeekly> findByYearAndWeekOrderByCompositeScoreDesc(
            @Param("year") int year,
            @Param("week") int week,
            Pageable pageable
    );

    /**
     * 특정 상품의 주간 랭킹 조회
     */
    Optional<ProductMetricsWeekly> findByYearAndWeekAndProductId(int year, int week, Long productId);

    /**
     * 특정 년도/주차의 전체 상품 수
     */
    long countByYearAndWeek(int year, int week);
}
