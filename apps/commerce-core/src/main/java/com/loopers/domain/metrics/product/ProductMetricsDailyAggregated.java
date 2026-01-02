package com.loopers.domain.metrics.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 일별 메트릭 집계 결과 DTO
 * 배치에서 사용하기 위한 집계 데이터
 */
@Getter
@AllArgsConstructor
public class ProductMetricsDailyAggregated {
    private Long productId;
    private Long totalLikeCount;
    private Long totalViewCount;
    private Long totalSoldCount;

    public double calculateScore() {
        return totalViewCount * 0.1 + totalLikeCount * 0.2 + totalSoldCount * 0.7;
    }
}


