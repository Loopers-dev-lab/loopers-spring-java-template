package com.loopers.domain.ranking;

import java.util.List;

/**
 * 월간 랭킹 Repository 인터페이스
 */
public interface MonthlyRankRepository {

    /**
     * 월간 랭킹 엔티티를 저장합니다.
     */
    MonthlyRankEntity save(MonthlyRankEntity entity);

    /**
     * 월간 랭킹 엔티티 목록을 저장합니다.
     */
    List<MonthlyRankEntity> saveAll(List<MonthlyRankEntity> entities);

    /**
     * 특정 월의 랭킹을 조회합니다.
     */
    List<MonthlyRankEntity> findByYearMonth(String yearMonth);

    /**
     * 특정 월의 랭킹을 페이지네이션하여 조회합니다.
     */
    List<MonthlyRankEntity> findByYearMonthWithPagination(String yearMonth, int page, int size);

    /**
     * 특정 월의 모든 랭킹을 삭제합니다. (멱등성 보장용)
     * 
     * @param yearMonth 삭제할 월 (예: "2024-12")
     * @return 삭제된 레코드 수
     */
    long deleteByYearMonth(String yearMonth);
}
