package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotHourlyRepository {
    
    Optional<RankingSnapshotHourly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotHourly> findTopByOrderBySnapshotTimeDesc();
    
    /**
     * 최신 스냅샷 조회 (최적화된 쿼리 - 서브쿼리 제거, 인덱스 활용)
     * 2단계 조회를 1단계로 통합
     */
    List<RankingSnapshotHourly> findLatestSnapshotOrderByRank();
    
    RankingSnapshotHourly save(RankingSnapshotHourly snapshot);
    
    void delete(RankingSnapshotHourly snapshot);
    
    long count();
}

