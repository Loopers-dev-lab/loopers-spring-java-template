package com.loopers.domain.metrics;

import java.util.List;
import java.util.Optional;

public interface ProductMetricsWeeklyRepository {
    /**
     * 특정 년도/주차의 랭킹 조회 (좋아요 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByLikeCountDesc(int year, int week, int limit);

    /**
     * 특정 년도/주차의 랭킹 조회 (조회수 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByViewCountDesc(int year, int week, int limit);

    /**
     * 특정 년도/주차의 랭킹 조회 (주문수 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByOrderCountDesc(int year, int week, int limit);

    /**
     * 특정 년도/주차의 랭킹 조회 (score 기준 정렬)
     */
    List<ProductMetricsWeekly> findByYearAndWeekOrderByCompositeScoreDesc(int year, int week, int limit);

    /**
     * 특정 상품의 주간 랭킹 조회
     */
    Optional<ProductMetricsWeekly> findByYearAndWeekAndProductId(int year, int week, Long productId);

    /**
     * 특정 년도/주차의 전체 상품 수
     */
    long countByYearAndWeek(int year, int week);
}
