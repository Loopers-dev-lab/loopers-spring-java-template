package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 상품 메트릭 Repository 인터페이스
 * <p>
 * Domain 계층의 순수한 Repository 인터페이스입니다.
 * Infrastructure 계층에서 JPA로 구현됩니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public interface ProductMetricsRepository {

    /**
     * 메트릭 저장
     *
     * @param metrics 저장할 메트릭 엔티티
     * @return 저장된 메트릭 엔티티
     */
    ProductMetricsEntity save(ProductMetricsEntity metrics);

    /**
     * 복합키로 메트릭 조회
     *
     * @param id 복합키 (productId + metricDate)
     * @return 메트릭 엔티티
     */
    Optional<ProductMetricsEntity> findById(ProductMetricsId id);

    /**
     * 상품 ID와 날짜로 메트릭 조회
     *
     * @param productId  상품 ID
     * @param metricDate 메트릭 날짜
     * @return 메트릭 엔티티
     */
    Optional<ProductMetricsEntity> findByProductIdAndMetricDate(Long productId, LocalDate metricDate);

    /**
     * 기간별 메트릭 조회 (배치용)
     *
     * @param startDate 시작 날짜
     * @param endDate   종료 날짜
     * @return 메트릭 엔티티 목록
     */
    List<ProductMetricsEntity> findByMetricDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 기간별 상품 집계 (배치용 - GROUP BY)
     *
     * @param startDate 시작 날짜
     * @param endDate   종료 날짜
     * @return 집계 결과 목록
     */
    List<ProductMetricsAggregation> aggregateByDateRange(LocalDate startDate, LocalDate endDate);
}
