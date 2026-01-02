package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotMonthlyRepository {
    
    Optional<RankingSnapshotMonthly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotMonthly> findTopByOrderBySnapshotTimeDesc();
    
    /**
     * 최신 스냅샷 조회 (최적화된 쿼리 - 서브쿼리 제거, 인덱스 활용)
     * 2단계 조회를 1단계로 통합
     */
    List<RankingSnapshotMonthly> findLatestSnapshotOrderByProductRank();

    /**
     * 특정 snapshot_time의 스냅샷 조회 (product_rank 기준)
     */
    List<RankingSnapshotMonthly> findBySnapshotTimeOrderByProductRank(LocalDateTime snapshotTime);
    
    RankingSnapshotMonthly save(RankingSnapshotMonthly snapshot);
    
    void delete(RankingSnapshotMonthly snapshot);
    
    long count();
}

