package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ProductMetrics JPA Repository.
 * <p>
 * 상품 메트릭 집계 데이터를 관리합니다.
 * commerce-batch 전용 Repository입니다.
 * </p>
 * <p>
 * <b>모듈별 독립성:</b>
 * <ul>
 *   <li>commerce-batch의 필요에 맞게 커스터마이징된 Repository</li>
 *   <li>Spring Batch에서 날짜 기반 조회에 최적화</li>
 * </ul>
 * </p>
 */
public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    /**
     * 상품 ID로 메트릭을 조회합니다.
     *
     * @param productId 상품 ID
     * @return 조회된 메트릭을 담은 Optional
     */
    Optional<ProductMetrics> findByProductId(Long productId);

    /**
     * 특정 날짜에 업데이트된 메트릭을 페이징하여 조회합니다.
     * <p>
     * Spring Batch의 JpaPagingItemReader에서 사용됩니다.
     * updated_at 필드를 기준으로 해당 날짜의 데이터만 조회합니다.
     * </p>
     * <p>
     * <b>주의:</b> 쿼리는 {@code updatedAt >= :startDateTime AND updatedAt < :endDateTime} 조건을 사용하므로,
     * endDateTime은 exclusive end입니다. 예를 들어, 2024-12-15의 데이터를 조회하려면:
     * <ul>
     *   <li>startDateTime: 2024-12-15 00:00:00</li>
     *   <li>endDateTime: 2024-12-16 00:00:00 (다음 날 00:00:00)</li>
     * </ul>
     * 또는 {@code date.atTime(LocalTime.MAX)}를 사용할 수도 있습니다.
     * </p>
     *
     * @param startDateTime 조회 시작 시각 (해당 날짜의 00:00:00, inclusive)
     * @param endDateTime 조회 종료 시각 (다음 날 00:00:00 또는 해당 날짜의 23:59:59.999999999, exclusive)
     * @param pageable 페이징 정보
     * @return 조회된 메트릭 페이지
     */
    @Query("SELECT pm FROM ProductMetrics pm " +
           "WHERE pm.updatedAt >= :startDateTime AND pm.updatedAt < :endDateTime " +
           "ORDER BY pm.productId")
    Page<ProductMetrics> findByUpdatedAtBetween(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
    );
}

