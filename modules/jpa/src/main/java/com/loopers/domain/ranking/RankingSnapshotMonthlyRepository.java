package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotMonthlyRepository {
    
    Optional<RankingSnapshotMonthly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotMonthly> findTopByOrderBySnapshotTimeDesc();
    
    RankingSnapshotMonthly save(RankingSnapshotMonthly snapshot);
}

