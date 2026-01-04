package com.loopers.domain.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ProductMetrics 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 상품 메트릭 집계 데이터의 영속성 계층과의 상호작용을 정의합니다.
 * DIP를 준수하여 도메인 레이어에서 인터페이스를 정의합니다.
 * </p>
 * <p>
 * <b>도메인 분리 근거:</b>
 * <ul>
 *   <li>Metric 도메인은 외부 시스템 연동을 위한 별도 관심사</li>
 *   <li>Product 도메인의 핵심 비즈니스 로직과는 분리</li>
 * </ul>
 * </p>
 * <p>
 * <b>배치 전용 메서드:</b>
 * <ul>
 *   <li>Spring Batch에서 날짜 기반 조회를 위한 메서드 포함</li>
 *   <li>대량 데이터 처리를 위한 페이징 조회 지원</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface ProductMetricsRepository {

    /**
     * 상품 메트릭을 저장합니다.
     *
     * @param productMetrics 저장할 상품 메트릭
     * @return 저장된 상품 메트릭
     */
    ProductMetrics save(ProductMetrics productMetrics);

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
    Page<ProductMetrics> findByUpdatedAtBetween(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Pageable pageable
    );

    /**
     * Spring Batch의 RepositoryItemReader에서 사용하기 위한 JPA Repository를 반환합니다.
     * <p>
     * RepositoryItemReader는 PagingAndSortingRepository를 직접 요구하므로,
     * 기술적 제약으로 인해 JPA Repository에 대한 접근을 제공합니다.
     * </p>
     * <p>
     * <b>주의:</b> 이 메서드는 Spring Batch의 기술적 요구사항으로 인해 제공됩니다.
     * 일반적인 비즈니스 로직에서는 이 메서드를 사용하지 않고,
     * 위의 도메인 메서드들을 사용해야 합니다.
     * </p>
     *
     * @return PagingAndSortingRepository를 구현한 JPA Repository
     */
    org.springframework.data.repository.PagingAndSortingRepository<ProductMetrics, Long> getJpaRepository();
}

