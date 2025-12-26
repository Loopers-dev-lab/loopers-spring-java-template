package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotMonthly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotMonthlyJpaRepository extends JpaRepository<RankingSnapshotMonthly, Long> {
    
    Optional<RankingSnapshotMonthly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotMonthly> findTopByOrderBySnapshotTimeDesc();
}

