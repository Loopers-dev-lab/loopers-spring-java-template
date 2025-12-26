package com.loopers.infrastructure.persistence;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {

    /**
     * 비관적 락을 사용한 조회 (동시성 제어)
     * - 동시에 여러 이벤트가 들어올 때 데이터 정합성 보장
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.productId = :productId")
    Optional<ProductMetrics> findByIdWithLock(@Param("productId") Long productId);

    /**
     * 좋아요 수 상위 N개 상품 조회
     */
    @Query("SELECT pm FROM ProductMetrics pm ORDER BY pm.likeCount DESC LIMIT :limit")
    List<ProductMetrics> findTopByLikeCount(@Param("limit") int limit);

    /**
     * 판매량 상위 N개 상품 조회
     */
    @Query("SELECT pm FROM ProductMetrics pm ORDER BY pm.salesCount DESC LIMIT :limit")
    List<ProductMetrics> findTopBySalesCount(@Param("limit") int limit);

    /**
     * 조회수 상위 N개 상품 조회
     */
    @Query("SELECT pm FROM ProductMetrics pm ORDER BY pm.viewCount DESC LIMIT :limit")
    List<ProductMetrics> findTopByViewCount(@Param("limit") int limit);
}
