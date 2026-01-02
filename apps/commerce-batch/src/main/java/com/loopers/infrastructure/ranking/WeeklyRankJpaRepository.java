package com.loopers.infrastructure.ranking;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankId;

/**
 * 주간 랭킹 JPA Repository
 */
public interface WeeklyRankJpaRepository extends JpaRepository<WeeklyRankEntity, WeeklyRankId> {

    /**
     * 특정 주차의 랭킹을 순위 순으로 조회합니다.
     */
    @Query("SELECT w FROM WeeklyRankEntity w WHERE w.id.yearWeek = :yearWeek ORDER BY w.rankPosition ASC")
    List<WeeklyRankEntity> findByIdYearWeekOrderByRankPosition(@Param("yearWeek") String yearWeek);

    /**
     * 특정 주차의 랭킹을 순위 순으로 페이지네이션하여 조회합니다.
     */
    @Query("SELECT w FROM WeeklyRankEntity w WHERE w.id.yearWeek = :yearWeek ORDER BY w.rankPosition ASC")
    Page<WeeklyRankEntity> findByIdYearWeekOrderByRankPosition(@Param("yearWeek") String yearWeek, Pageable pageable);

    /**
     * 특정 주차의 모든 랭킹을 삭제합니다.
     *
     * @param yearWeek 삭제할 주차
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM WeeklyRankEntity w WHERE w.id.yearWeek = :yearWeek")
    long deleteByIdYearWeek(@Param("yearWeek") String yearWeek);
}
