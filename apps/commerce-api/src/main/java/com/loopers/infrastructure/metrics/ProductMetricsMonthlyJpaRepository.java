package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductMetricsMonthlyJpaRepository extends JpaRepository<ProductMetricsMonthly, Long> {

    /**
     * 특정 년도/월의 랭킹 조회 (좋아요 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByTotalLikeCountDesc(int year, int month, Pageable pageable);

    /**
     * 특정 년도/월의 랭킹 조회 (조회수 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByTotalViewCountDesc(int year, int month, Pageable pageable);

    /**
     * 특정 년도/월의 랭킹 조회 (주문수 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByTotalOrderCountDesc(int year, int month, Pageable pageable);

    /**
     * 특정 년도/월의 랭킹 조회 (종합 점수 기준 정렬)
     * 종합 점수 = (like * 0.2) + (view * 0.1) + (order * 0.6)
     */
    @Query("""
        SELECT m FROM ProductMetricsMonthly m
        WHERE m.year = :year AND m.month = :month
        ORDER BY (m.totalLikeCount * 0.2 + m.totalViewCount * 0.1 + m.totalOrderCount * 0.6) DESC
        """)
    List<ProductMetricsMonthly> findByYearAndMonthOrderByCompositeScoreDesc(
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    /**
     * 특정 상품의 월간 랭킹 조회
     */
    Optional<ProductMetricsMonthly> findByYearAndMonthAndProductId(int year, int month, Long productId);

    /**
     * 특정 년도/월의 전체 상품 수
     */
    long countByYearAndMonth(int year, int month);
}
