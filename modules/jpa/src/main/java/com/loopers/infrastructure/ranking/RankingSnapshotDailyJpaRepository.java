package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotDailyJpaRepository extends JpaRepository<RankingSnapshotDaily, Long> {
    
    Optional<RankingSnapshotDaily> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotDaily> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotDaily> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotDaily> findTopByOrderBySnapshotTimeDesc();

    /**
     * 최신 스냅샷 조회 (최적화된 쿼리 - 서브쿼리 제거, 인덱스 활용)
     * 인덱스 (snapshot_time DESC, rank ASC) 활용
     */
    @Query(value = "SELECT * FROM ranking_snapshot_daily " +
           "ORDER BY snapshot_time DESC, rank ASC LIMIT 500",
           nativeQuery = true)
    List<RankingSnapshotDaily> findLatestSnapshotOrderByRank();

    /**
     * 특정 snapshot_time의 스냅샷 조회 (rank 기준)
     */
    @Query("SELECT s FROM RankingSnapshotDaily s " +
           "WHERE s.snapshotTime = :snapshotTime " +
           "ORDER BY s.rank ASC")
    List<RankingSnapshotDaily> findBySnapshotTimeOrderByRank(@Param("snapshotTime") LocalDateTime snapshotTime);
}

