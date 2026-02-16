package com.loopers.domain.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 주간 집계 데이터 DTO
 * Spring Batch ItemReader에서 Repository 조회 결과로 사용됨
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAggregationDto {
    private Long productId;
    private Integer year;
    private Integer week;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Long totalLikeCount;
    private Long totalViewCount;
    private Long totalOrderCount;
    private Long totalOrderQuantity;
}