package com.loopers.batch.metrics;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.dto.MonthlyAggregationDto;
import org.springframework.batch.item.ItemProcessor;

/**
 * 월간 집계 DTO를 ProductMetricsMonthly 엔티티로 변환하는 Processor
 */
public class MonthlyMetricsProcessor implements ItemProcessor<MonthlyAggregationDto, ProductMetricsMonthly> {
    @Override
    public ProductMetricsMonthly process(MonthlyAggregationDto dto) {
        // DTO를 도메인 엔티티로 변환
        ProductMetricsMonthly metrics = ProductMetricsMonthly.create(
                dto.getProductId(),
                dto.getYear(),
                dto.getMonth(),
                dto.getPeriodStartDate(),
                dto.getPeriodEndDate()
        );

        // 집계 메트릭 업데이트
        metrics.updateMetrics(
                dto.getTotalLikeCount(),
                dto.getTotalViewCount(),
                dto.getTotalOrderCount()
        );

        return metrics;
    }
}
