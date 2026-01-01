package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MvProductRankMonthlyRepository extends JpaRepository<MvProductRankMonthly, Long> {

    /**
     * 월간 랭킹 조회 (순위별 정렬)
     */
    Page<MvProductRankMonthly> findByYearMonthOrderByRanking(@Param("yearMonth") String yearMonth, Pageable pageable);

    /**
     * 특정 기간의 TOP 랭킹 조회
     */
    @Query("SELECT mv FROM MvProductRankMonthly mv WHERE mv.yearMonth = :yearMonth AND mv.ranking <= :topN ORDER BY mv.ranking")
    List<MvProductRankMonthly> findTopNByYearMonth(@Param("yearMonth") String yearMonth, @Param("topN") int topN);

    /**
     * 특정 상품의 월간 랭킹 조회
     */
    @Query("SELECT mv FROM MvProductRankMonthly mv WHERE mv.yearMonth = :yearMonth AND mv.productId = :productId")
    MvProductRankMonthly findByYearMonthAndProductId(@Param("yearMonth") String yearMonth, @Param("productId") Long productId);

    /**
     * 해당 기간에 랭킹 데이터가 있는지 확인
     */
    boolean existsByYearMonth(String yearMonth);

    /**
     * 특정 기간의 전체 랭킹 개수 조회
     */
    long countByYearMonth(String yearMonth);
}
