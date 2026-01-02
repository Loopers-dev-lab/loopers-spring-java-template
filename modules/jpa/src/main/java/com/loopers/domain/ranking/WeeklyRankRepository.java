package com.loopers.domain.ranking;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 주간 랭킹 Repository 인터페이스
 */
public interface WeeklyRankRepository {

    /**
     * 주간 랭킹 엔티티를 저장합니다.
     */
    WeeklyRankEntity save(WeeklyRankEntity entity);

    /**
     * 주간 랭킹 엔티티 목록을 저장합니다.
     */
    List<WeeklyRankEntity> saveAll(List<WeeklyRankEntity> entities);

    /**
     * 특정 주차의 랭킹을 조회합니다.
     */
    List<WeeklyRankEntity> findByYearWeek(String yearWeek);

    /**
     * 특정 주차의 모든 랭킹을 삭제합니다. (멱등성 보장용)
     * 
     * @param yearWeek 삭제할 주차 (예: "2024-W52")
     * @return 삭제된 레코드 수
     */
    long deleteByYearWeek(String yearWeek);

    Page<WeeklyRankEntity> findByYearWeek(String yearWeek, Pageable pageable);
}
