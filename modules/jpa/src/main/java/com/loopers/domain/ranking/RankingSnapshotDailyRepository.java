package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotDailyRepository {
    
    Optional<RankingSnapshotDaily> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotDaily> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotDaily> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotDaily> findTopByOrderBySnapshotTimeDesc();
    
    RankingSnapshotDaily save(RankingSnapshotDaily snapshot);
}

