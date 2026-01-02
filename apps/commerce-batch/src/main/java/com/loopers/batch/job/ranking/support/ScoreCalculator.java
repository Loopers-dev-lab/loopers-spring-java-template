package com.loopers.batch.job.ranking.support;

import org.springframework.stereotype.Component;

/**
 * 랭킹 점수 계산기
 * - Redis ZSET과 동일한 가중치 적용
 * - 점수 = viewCount*1 + likeCount*3 + salesCount*5 + orderCount*2
 */
@Component
public class ScoreCalculator {

    // CachePayloads의 EventType 가중치와 일치하도록 조정 (비율 유지)
    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.2;
    private static final double SALES_WEIGHT = 0.6; // 주문(결제성공) 가중치

    /**
     * 메트릭 데이터를 기반으로 랭킹 점수를 계산합니다.
     *
     * @param viewCount  조회수
     * @param likeCount  좋아요수
     * @param salesCount 판매수량
     * @param orderCount 주문수
     * @return 계산된 총 점수
     */
    public long calculate(long viewCount, long likeCount, long salesCount, long orderCount) {
        // 1. 조회와 좋아요는 단순 수량 기반 가중치 적용
        double viewScore = viewCount * VIEW_WEIGHT;
        double likeScore = likeCount * LIKE_WEIGHT;

        // 2. 판매량(Sales)은 CachePayloads.forPaymentSuccess와 동일하게 로그 정규화 적용
        // 배치에서는 이미 집계된 salesCount(수량)를 기반으로 하므로,
        // 만약 금액 기반 정규화가 필요하다면 매개변수로 총액을 받아야 하지만,
        // 수량 기반으로 로그 정규화를 적용한다면 아래와 같이 작성합니다.
        double normalizedSalesScore = Math.log1p(salesCount) * SALES_WEIGHT;

        // 3. 최종 점수 계산 (소수점 처리를 위해 적절한 스케일 곱산 후 long 변환)
        // Redis ZSET의 score가 double임을 감안하여 정밀도를 유지합니다.
        return (long) ((viewScore + likeScore + normalizedSalesScore) * 1000);
    }

    /**
     * 가중치 정보를 반환합니다. (테스트 및 디버깅용)
     */
    public String getWeightInfo() {
        return String.format("VIEW=%f, LIKE=%f, SALES=%f,",
                VIEW_WEIGHT, LIKE_WEIGHT, SALES_WEIGHT);
    }
}
