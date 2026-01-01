package com.loopers.domain.rank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * MonthlyProductRank 엔티티를 위한 Repository 인터페이스.
 *
 * <p>월간 상품 랭킹 데이터에 대한 데이터 접근 메서드를 제공합니다.
 */
public interface MonthlyRankRepository extends JpaRepository<MonthlyProductRank, Long> {

    /**
     * 특정 월의 모든 랭킹을 순위 순서대로 조회합니다.
     *
     * @param yearMonth 년-월 (예: "2025-01")
     * @return 순위순으로 정렬된 월간 랭킹 목록
     */
    List<MonthlyProductRank> findByYearMonthOrderByRankPositionAsc(String yearMonth);

    /**
     * 특정 월의 모든 랭킹을 삭제합니다.
     *
     * <p>데이터 일관성을 보장하기 위해 새로운 집계 데이터를 삽입하기 전에 사용됩니다.
     *
     * @param yearMonth 삭제할 년-월
     */
    @Modifying
    @Query("DELETE FROM MonthlyProductRank m WHERE m.yearMonth = :yearMonth")
    void deleteByYearMonth(@Param("yearMonth") String yearMonth);
}
