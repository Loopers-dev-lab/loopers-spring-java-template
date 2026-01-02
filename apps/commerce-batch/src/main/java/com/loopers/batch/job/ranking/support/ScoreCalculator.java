package com.loopers.batch.job.ranking.support;

import org.springframework.stereotype.Component;

/**
 * 랭킹 점수 계산기
 * - Redis ZSET과 동일한 가중치 적용
 * - 점수 = viewCount*1 + likeCount*3 + salesCount*5 + orderCount*2
 */
@Component
public class ScoreCalculator {

    // 가중치 상수 (Redis ZSET과 동일하게 유지)
    private static final int VIEW_WEIGHT = 1;
    private static final int LIKE_WEIGHT = 3;
    private static final int SALES_WEIGHT = 5;
    private static final int ORDER_WEIGHT = 2;

    /**
     * 메트릭 데이터를 기반으로 랭킹 점수를 계산합니다.
     *
     * @param viewCount 조회수
     * @param likeCount 좋아요수
     * @param salesCount 판매수량
     * @param orderCount 주문수
     * @return 계산된 총 점수
     */
    public long calculate(long viewCount, long likeCount, long salesCount, long orderCount) {
        return viewCount * VIEW_WEIGHT
             + likeCount * LIKE_WEIGHT
             + salesCount * SALES_WEIGHT
             + orderCount * ORDER_WEIGHT;
    }

    /**
     * 가중치 정보를 반환합니다. (테스트 및 디버깅용)
     */
    public String getWeightInfo() {
        return String.format("VIEW=%d, LIKE=%d, SALES=%d, ORDER=%d", 
            VIEW_WEIGHT, LIKE_WEIGHT, SALES_WEIGHT, ORDER_WEIGHT);
    }
}