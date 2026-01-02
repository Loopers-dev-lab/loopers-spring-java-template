package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotMonthlyJpaRepository extends JpaRepository<RankingSnapshotMonthly, Long> {

    Optional<RankingSnapshotMonthly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotMonthly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotMonthly> findTopByOrderBySnapshotTimeDesc();

    /**
     * 최신 스냅샷 조회 (최적화된 쿼리 - 서브쿼리 제거, 인덱스 활용)
     * 인덱스 (snapshot_time DESC, rank ASC) 활용
     */
    @Query(value = "SELECT * FROM ranking_snapshot_monthly " +
           "ORDER BY snapshot_time DESC, rank ASC LIMIT 500",
           nativeQuery = true)
    List<RankingSnapshotMonthly> findLatestSnapshotOrderByRank();

    /**
     * 특정 snapshot_time의 스냅샷 조회 (rank 기준)
     */
    @Query("SELECT s FROM RankingSnapshotMonthly s " +
           "WHERE s.snapshotTime = :snapshotTime " +
           "ORDER BY s.rank ASC")
    List<RankingSnapshotMonthly> findBySnapshotTimeOrderByRank(@Param("snapshotTime") LocalDateTime snapshotTime);
}

