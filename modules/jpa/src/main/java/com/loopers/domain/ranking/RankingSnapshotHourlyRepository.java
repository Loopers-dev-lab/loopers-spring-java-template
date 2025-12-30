package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotHourlyRepository {
    
    Optional<RankingSnapshotHourly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotHourly> findTopByOrderBySnapshotTimeDesc();
    
    RankingSnapshotHourly save(RankingSnapshotHourly snapshot);
    
    void delete(RankingSnapshotHourly snapshot);
    
    long count();
}

