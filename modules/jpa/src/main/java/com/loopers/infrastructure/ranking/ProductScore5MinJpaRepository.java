package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.ProductScore5Min;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductScore5MinJpaRepository extends JpaRepository<ProductScore5Min, Long> {

    /**
     * 최근 30일 내의 최대 end_time 조회 (last_processed_time 파악용)
     */
    @Query("SELECT MAX(p.endTime) FROM ProductScore5Min p " +
           "WHERE p.endTime >= :cutoffDate")
    Optional<LocalDateTime> findMaxEndTimeAfter(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 특정 시간 범위의 5분 집계 데이터 조회
     */
    @Query("SELECT p FROM ProductScore5Min p " +
           "WHERE p.startTime >= :startTime AND p.endTime <= :endTime " +
           "ORDER BY p.startTime")
    List<ProductScore5Min> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 특정 product_id와 시간 범위로 기존 데이터 조회 (중복 체크용)
     */
    @Query("SELECT p FROM ProductScore5Min p " +
           "WHERE p.productId = :productId " +
           "AND p.startTime = :startTime " +
           "AND p.endTime = :endTime")
    Optional<ProductScore5Min> findByProductIdAndTimeRange(
        @Param("productId") Long productId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 여러 product_id와 시간 범위로 기존 데이터 배치 조회
     */
    @Query("SELECT p FROM ProductScore5Min p " +
           "WHERE (p.productId, p.startTime, p.endTime) IN " +
           "(SELECT p2.productId, p2.startTime, p2.endTime FROM ProductScore5Min p2 " +
           "WHERE p2.productId IN :productIds)")
    List<ProductScore5Min> findByProductIdsAndTimeRanges(@Param("productIds") List<Long> productIds);

    /**
     * 단일 UPDATE (불변 객체이므로 네이티브 쿼리 사용)
     */
    @Modifying
    @Query(value = "UPDATE product_score_5min " +
           "SET order_amount_sum = order_amount_sum + :orderAmountSum, " +
           "    like_count = like_count + :likeCount, " +
           "    view_count = view_count + :viewCount, " +
           "    updated_at = NOW() " +
           "WHERE product_id = :productId " +
           "  AND start_time = :startTime " +
           "  AND end_time = :endTime",
           nativeQuery = true)
    int updateProductScore5Min(
        @Param("productId") Long productId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("orderAmountSum") BigDecimal orderAmountSum,
        @Param("likeCount") Long likeCount,
        @Param("viewCount") Long viewCount
    );

    /**
     * 배치 UPDATE를 위한 메서드
     * EntityManager를 통해 배치로 처리하기 위해 사용
     * clearAutomatically와 flushAutomatically를 false로 설정하여
     * Aggregate5MinWriter에서 수동으로 flush/clear를 제어
     */
    @Modifying(clearAutomatically = false, flushAutomatically = false)
    @Query(value = "UPDATE product_score_5min " +
           "SET order_amount_sum = order_amount_sum + :orderAmountSum, " +
           "    like_count = like_count + :likeCount, " +
           "    view_count = view_count + :viewCount, " +
           "    updated_at = NOW() " +
           "WHERE product_id = :productId " +
           "  AND start_time = :startTime " +
           "  AND end_time = :endTime",
           nativeQuery = true)
    int updateProductScore5MinForBatch(
        @Param("productId") Long productId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("orderAmountSum") BigDecimal orderAmountSum,
        @Param("likeCount") Long likeCount,
        @Param("viewCount") Long viewCount
    );

}

