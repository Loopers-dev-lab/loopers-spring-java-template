package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotDailyJpaRepository extends JpaRepository<RankingSnapshotDaily, Long> {
    
    Optional<RankingSnapshotDaily> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotDaily> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotDaily> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotDaily> findTopByOrderBySnapshotTimeDesc();
}

