package com.loopers.application.batch.dto;

/**
 * 점수가 계산된 상품 정보
 * - Processor가 생성하는 중간 데이터
 * - primitive 타입 사용으로 NPE 방지
 */
public record RankedProductDto(
    long productId,
    double totalScore,
    long likeCount,
    long viewCount,
    long salesCount,
    long salesAmount
) implements Comparable<RankedProductDto> {

    @Override
    public int compareTo(RankedProductDto other) {
        // 점수 내림차순 정렬
        return Double.compare(other.totalScore, this.totalScore);
    }
}
