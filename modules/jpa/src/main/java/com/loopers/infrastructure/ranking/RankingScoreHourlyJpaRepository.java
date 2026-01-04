package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingScoreHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RankingScoreHourlyJpaRepository extends JpaRepository<RankingScoreHourly, Long> {

    /**
     * current_score 기준 상위 N개 조회
     */
    @Query("SELECT r FROM RankingScoreHourly r " +
           "ORDER BY r.currentScore DESC")
    List<RankingScoreHourly> findTopByOrderByCurrentScoreDesc(int limit);

    /**
     * 배치 UPSERT를 위한 네이티브 쿼리
     * MySQL의 INSERT ... ON DUPLICATE KEY UPDATE 사용
     */
    @Modifying
    @Query(value = "INSERT INTO ranking_score_hourly " +
           "(product_id, total_order_amount, total_like_count, total_view_count, current_score, last_processed_time, created_at, updated_at) " +
           "VALUES (:productId, :totalOrderAmount, :totalLikeCount, :totalViewCount, :currentScore, :lastProcessedTime, NOW(), NOW()) " +
           "ON DUPLICATE KEY UPDATE " +
           "total_order_amount = total_order_amount + :newOrderAmount - :oldOrderAmount, " +
           "total_like_count = total_like_count + :newLikeCount - :oldLikeCount, " +
           "total_view_count = total_view_count + :newViewCount - :oldViewCount, " +
           "current_score = :currentScore, " +
           "last_processed_time = :lastProcessedTime, " +
           "updated_at = NOW()",
           nativeQuery = true)
    void upsertRankingScore(
        @Param("productId") Long productId,
        @Param("totalOrderAmount") java.math.BigDecimal totalOrderAmount,
        @Param("totalLikeCount") Long totalLikeCount,
        @Param("totalViewCount") Long totalViewCount,
        @Param("currentScore") Double currentScore,
        @Param("lastProcessedTime") java.time.LocalDateTime lastProcessedTime,
        @Param("newOrderAmount") java.math.BigDecimal newOrderAmount,
        @Param("oldOrderAmount") java.math.BigDecimal oldOrderAmount,
        @Param("newLikeCount") Long newLikeCount,
        @Param("oldLikeCount") Long oldLikeCount,
        @Param("newViewCount") Long newViewCount,
        @Param("oldViewCount") Long oldViewCount
    );
}

