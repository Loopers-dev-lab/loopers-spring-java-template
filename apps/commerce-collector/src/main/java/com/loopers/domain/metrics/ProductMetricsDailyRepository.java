package com.loopers.domain.metrics;

import com.loopers.application.order.OrderMetrics;
import com.loopers.domain.metrics.dto.MonthlyAggregationDto;
import com.loopers.domain.metrics.dto.WeeklyAggregationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductMetricsDailyRepository {
    Optional<ProductMetricsDaily> findByProductIdAndMetricDate(Long productId, LocalDate metricDate);
    List<ProductMetricsDaily> findAllByMetricDateAndIsProcessed(LocalDate metricDate, boolean isProcessed);
    ProductMetricsDaily save(ProductMetricsDaily daily);
    void saveAll(List<ProductMetricsDaily> unprocessedRecords);

    // 일자별 증감 배치 업데이트 (UPSERT)
    void upsertLikeDeltas(Map<Long, Integer> likeDeltas, LocalDate metricDate);
    void upsertViewDeltas(Map<Long, Integer> viewDeltas, LocalDate metricDate);
    void upsertOrderDeltas(Map<Long, OrderMetrics> orderMetrics, LocalDate metricDate);

    // 오래된 데이터 삭제
    int deleteByMetricDateBefore(LocalDate cutoffDate);

    List<ProductMetricsDaily> findAllByMetricDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 주간 집계 데이터 조회 (페이징)
     * Spring Batch Job에서 사용
     *
     * @param year 집계 연도
     * @param week 집계 주차
     * @param startDate 집계 시작일 (월요일)
     * @param endDate 집계 종료일 (일요일)
     * @param pageable 페이징 정보
     * @return 상품별 주간 집계 결과
     */
    Page<WeeklyAggregationDto> findWeeklyAggregation(
            Integer year,
            Integer week,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * 월간 집계 데이터 조회 (페이징)
     * Spring Batch Job에서 사용
     *
     * @param year 집계 연도
     * @param month 집계 월
     * @param startDate 집계 시작일 (1일)
     * @param endDate 집계 종료일 (말일)
     * @param pageable 페이징 정보
     * @return 상품별 월간 집계 결과
     */
    Page<MonthlyAggregationDto> findMonthlyAggregation(
            Integer year,
            Integer month,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
}
