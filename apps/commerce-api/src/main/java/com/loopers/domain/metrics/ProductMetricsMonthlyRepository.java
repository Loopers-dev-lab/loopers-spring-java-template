package com.loopers.domain.metrics;

import java.util.List;
import java.util.Optional;

public interface ProductMetricsMonthlyRepository {
    /**
     * 특정 년도/월의 랭킹 조회 (좋아요 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByLikeCountDesc(int year, int month, int limit);

    /**
     * 특정 년도/월의 랭킹 조회 (조회수 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByViewCountDesc(int year, int month, int limit);

    /**
     * 특정 년도/월의 랭킹 조회 (주문수 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByOrderCountDesc(int year, int month, int limit);

    /**
     * 특정 년도/월의 랭킹 조회 (Score 기준 정렬)
     */
    List<ProductMetricsMonthly> findByYearAndMonthOrderByCompositeScoreDesc(int year, int month, int limit);

    /**
     * 특정 상품의 월간 랭킹 조회
     */
    Optional<ProductMetricsMonthly> findByYearAndMonthAndProductId(int year, int month, Long productId);

    /**
     * 특정 년도/월의 전체 상품 수
     */
    long countByYearAndMonth(int year, int month);
}
