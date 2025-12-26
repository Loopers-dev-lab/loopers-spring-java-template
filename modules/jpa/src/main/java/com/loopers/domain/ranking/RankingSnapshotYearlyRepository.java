package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotYearlyRepository {
    
    Optional<RankingSnapshotYearly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotYearly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotYearly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotYearly> findTopByOrderBySnapshotTimeDesc();
    
    RankingSnapshotYearly save(RankingSnapshotYearly snapshot);
}

