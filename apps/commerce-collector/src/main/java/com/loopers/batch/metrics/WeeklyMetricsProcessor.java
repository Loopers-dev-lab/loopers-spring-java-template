package com.loopers.batch.metrics;

import com.loopers.domain.metrics.ProductMetricsWeekly;
import com.loopers.domain.metrics.dto.WeeklyAggregationDto;
import org.springframework.batch.item.ItemProcessor;

/**
 * 주간 집계 DTO를 ProductMetricsWeekly 엔티티로 변환하는 Processor
 */
public class WeeklyMetricsProcessor implements ItemProcessor<WeeklyAggregationDto, ProductMetricsWeekly> {
    @Override
    public ProductMetricsWeekly process(WeeklyAggregationDto dto) {
        // DTO를 도메인 엔티티로 변환
        ProductMetricsWeekly metrics = ProductMetricsWeekly.create(
                dto.getProductId(),
                dto.getYear(),
                dto.getWeek(),
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
