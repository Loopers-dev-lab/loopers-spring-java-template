package com.loopers.domain.metrics;

import com.loopers.application.order.OrderMetrics;

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
}
