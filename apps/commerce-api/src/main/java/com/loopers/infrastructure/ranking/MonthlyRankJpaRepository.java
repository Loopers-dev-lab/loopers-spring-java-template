package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 월간 랭킹 JPA Repository (commerce-api용)
 */
public interface MonthlyRankJpaRepository extends JpaRepository<MonthlyRankEntity, MonthlyRankId> {

    /**
     * 특정 월의 랭킹을 순위 순으로 조회합니다.
     */
    @Query("SELECT m FROM MonthlyRankEntity m WHERE m.id.yearMonth = :yearMonth ORDER BY m.rankPosition ASC")
    List<MonthlyRankEntity> findByIdYearMonthOrderByRankPosition(@Param("yearMonth") String yearMonth);

    /**
     * 특정 월의 랭킹을 순위 순으로 페이지네이션하여 조회합니다.
     */
    @Query("SELECT m FROM MonthlyRankEntity m WHERE m.id.yearMonth = :yearMonth ORDER BY m.rankPosition ASC")
    List<MonthlyRankEntity> findByIdYearMonthOrderByRankPosition(@Param("yearMonth") String yearMonth, Pageable pageable);

    /**
     * 특정 월의 모든 랭킹을 삭제합니다.
     * 
     * @param yearMonth 삭제할 월
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM MonthlyRankEntity m WHERE m.id.yearMonth = :yearMonth")
    long deleteByIdYearMonth(@Param("yearMonth") String yearMonth);
}