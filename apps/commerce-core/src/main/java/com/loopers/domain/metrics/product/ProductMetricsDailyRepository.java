package com.loopers.domain.metrics.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsDailyRepository {
    /**
     * 특정 상품의 특정 날짜 메트릭 조회
     */
    Optional<ProductMetricsDaily> findByProductIdAndDate(Long productId, LocalDate date);
    
    /**
     * 특정 상품의 특정 날짜 메트릭 조회 (락)
     */
    Optional<ProductMetricsDaily> findByProductIdAndDateForUpdate(Long productId, LocalDate date);
    
    /**
     * 특정 날짜 범위의 메트릭 조회 (배치용)
     */
    List<ProductMetricsDaily> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 특정 날짜 범위의 메트릭을 product_id별로 집계하여 조회 (배치용)
     */
    List<ProductMetricsDailyAggregated> findAggregatedByDateBetween(
        LocalDate startDate, 
        LocalDate endDate
    );

    /**
     * 특정 날짜 범위의 메트릭을 product_id별로 집계하여 페이징 조회 (배치용)
     */
    Page<ProductMetricsDailyAggregated> findAggregatedByDateBetweenPaged(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * 메트릭 저장
     */
    ProductMetricsDaily save(ProductMetricsDaily daily);
    
    /**
     * 메트릭 일괄 저장
     */
    List<ProductMetricsDaily> saveAll(Collection<ProductMetricsDaily> dailies);
}


