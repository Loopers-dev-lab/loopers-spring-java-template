package com.loopers.application.batch.dto;

/**
 * Batch 처리용 ProductMetrics DTO
 * - JdbcItemReader가 읽어온 데이터를 담는 객체
 * - primitive long 타입 사용으로 NPE 방지 (DB NULL은 0으로 변환됨)
 */
public record ProductMetricsDto(
    long productId,
    long likeCount,
    long viewCount,
    long salesCount,
    long salesAmount
) {
}
