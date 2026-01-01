package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MvProductRankWeeklyRepository extends JpaRepository<MvProductRankWeekly, Long> {

    /**
     * 주간 랭킹 조회 (순위별 정렬)
     */
    Page<MvProductRankWeekly> findByYearMonthWeekOrderByRanking(@Param("yearMonthWeek") String yearMonthWeek, Pageable pageable);

    /**
     * 특정 기간의 TOP 랭킹 조회
     */
    @Query("SELECT mv FROM MvProductRankWeekly mv WHERE mv.yearMonthWeek = :yearMonthWeek AND mv.ranking <= :topN ORDER BY mv.ranking")
    List<MvProductRankWeekly> findTopNByYearMonthWeek(@Param("yearMonthWeek") String yearMonthWeek, @Param("topN") int topN);

    /**
     * 특정 상품의 주간 랭킹 조회
     */
    @Query("SELECT mv FROM MvProductRankWeekly mv WHERE mv.yearMonthWeek = :yearMonthWeek AND mv.productId = :productId")
    MvProductRankWeekly findByYearMonthWeekAndProductId(@Param("yearMonthWeek") String yearMonthWeek, @Param("productId") Long productId);

    /**
     * 해당 기간에 랭킹 데이터가 있는지 확인
     */
    boolean existsByYearMonthWeek(String yearMonthWeek);

    /**
     * 특정 기간의 전체 랭킹 개수 조회
     */
    long countByYearMonthWeek(String yearMonthWeek);
}
