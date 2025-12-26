package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotYearly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotYearlyJpaRepository extends JpaRepository<RankingSnapshotYearly, Long> {
    
    Optional<RankingSnapshotYearly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotYearly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotYearly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotYearly> findTopByOrderBySnapshotTimeDesc();
}

