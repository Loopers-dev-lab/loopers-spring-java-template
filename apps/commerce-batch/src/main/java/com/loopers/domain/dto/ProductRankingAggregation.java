package com.loopers.domain.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 상품 랭킹 집계 결과를 담는 Data Transfer Object.
 *
 * <p>이 DTO는 데이터베이스 쿼리에서 집계된 지표 데이터를 배치 프로세서로 전달하여
 * 추가 계산 및 랭킹 할당을 수행합니다.
 */
@Getter
@AllArgsConstructor
public class ProductRankingAggregation {

    /**
     * 상품 ID
     */
    private Long productId;

    /**
     * 기간 내 총 좋아요 수
     */
    private Integer likeCount;

    /**
     * 기간 내 총 조회 수
     */
    private Integer viewCount;

    /**
     * 기간 내 총 주문 수
     */
    private Integer orderCount;

    /**
     * 기간 내 총 판매 금액
     */
    private BigDecimal salesAmount;

    /**
     * 계산된 랭킹 순위 (1부터 시작)
     */
    private Integer rankPosition;
}
