package com.loopers.domain.ranking;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface MvProductRankWeeklyRepository {
    /**
     * 특정 날짜의 주간 랭킹 TOP 100 조회 (rank 오름차순)
     */
    List<MvProductRankWeekly> findTop100ByRankingDateOrderByRankAsc(ZonedDateTime rankingDate);

    /**
     * 특정 상품의 주간 랭킹 조회
     */
    Optional<MvProductRankWeekly> findByProductIdAndRankingDate(Long productId, ZonedDateTime rankingDate);

    /**
     * 특정 날짜의 주간 랭킹 데이터 삭제 (배치 재실행 전)
     */
    void deleteByRankingDate(ZonedDateTime rankingDate);

    /**
     * 주간 랭킹 데이터 일괄 저장
     */
    void saveAll(List<MvProductRankWeekly> rankings);
}
