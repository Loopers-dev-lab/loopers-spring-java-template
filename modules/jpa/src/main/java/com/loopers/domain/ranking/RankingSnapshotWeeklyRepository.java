package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotWeeklyRepository {
    
    Optional<RankingSnapshotWeekly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotWeekly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotWeekly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotWeekly> findTopByOrderBySnapshotTimeDesc();
    
    /**
     * 최신 스냅샷 조회 (최적화된 쿼리 - 서브쿼리 제거, 인덱스 활용)
     * 2단계 조회를 1단계로 통합
     */
    List<RankingSnapshotWeekly> findLatestSnapshotOrderByRank();
    
    RankingSnapshotWeekly save(RankingSnapshotWeekly snapshot);
    
    void delete(RankingSnapshotWeekly snapshot);
    
    long count();
}

