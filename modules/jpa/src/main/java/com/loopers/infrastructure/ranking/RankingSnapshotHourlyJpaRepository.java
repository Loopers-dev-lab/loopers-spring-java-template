package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotHourly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotHourlyJpaRepository extends JpaRepository<RankingSnapshotHourly, Long> {
    
    Optional<RankingSnapshotHourly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotHourly> findTopByOrderBySnapshotTimeDesc();
}

