package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotHourlyJpaRepository extends JpaRepository<RankingSnapshotHourly, Long> {
    
    Optional<RankingSnapshotHourly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime);
    
    List<RankingSnapshotHourly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<RankingSnapshotHourly> findTopByOrderBySnapshotTimeDesc();

    /**
     * 최신 스냅샷 조회 (최적화된 쿼리 - 서브쿼리 제거, 인덱스 활용)
     * 인덱스 (snapshot_time DESC, product_rank ASC) 활용
     */
    @Query(value = "SELECT * FROM ranking_snapshot_hourly " +
           "ORDER BY snapshot_time DESC, product_rank ASC LIMIT 500",
           nativeQuery = true)
    List<RankingSnapshotHourly> findLatestSnapshotOrderByProductRank();

    /**
     * 특정 snapshot_time의 스냅샷 조회 (product_rank 기준)
     */
    @Query("SELECT s FROM RankingSnapshotHourly s " +
           "WHERE s.snapshotTime = :snapshotTime " +
           "ORDER BY s.productRank ASC")
    List<RankingSnapshotHourly> findBySnapshotTimeOrderByProductRank(@Param("snapshotTime") LocalDateTime snapshotTime);
}

