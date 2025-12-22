package com.loopers.domain.ranking;

/**
 * 랭킹 점수 계산 전략
 * - 가중치 기반 점수 계산
 */
public class RankingScore {

    // 가중치 설정
    private static final double WEIGHT_VIEW = 0.1;     // 조회
    private static final double WEIGHT_LIKE = 0.2;     // 좋아요
    private static final double WEIGHT_ORDER = 0.6;    // 주문

    // 콜드 스타트 해결을 위한 전날 점수 가중치
    private static final double WEIGHT_CARRY_OVER = 0.1;  // 10%

    /**
     * 조회 이벤트 점수
     */
    public static double viewScore() {
        return WEIGHT_VIEW * 1;
    }

    /**
     * 좋아요 이벤트 점수
     */
    public static double likeScore() {
        return WEIGHT_LIKE * 1;
    }

    /**
     * 좋아요 취소 이벤트 점수 (음수)
     */
    public static double unlikeScore() {
        return -(WEIGHT_LIKE * 1);
    }

    /**
     * 주문 이벤트 점수
     * @param price 단가
     * @param amount 수량
     * @return 가중치 적용된 점수
     */
    public static double orderScore(int price, int amount) {
        // 로그 정규화 적용 (큰 값의 영향력 감소)
        double rawScore = price * amount;
        double normalizedScore = Math.log10(rawScore + 1);  // +1은 log(0) 방지
        return WEIGHT_ORDER * normalizedScore;
    }

    /**
     * 콜드 스타트를 위한 전날 점수 가중치
     */
    public static double carryOverWeight() {
        return WEIGHT_CARRY_OVER;
    }

    /**
     * 가중치 정보 조회
     */
    public static String getWeightInfo() {
        return String.format("View: %.1f, Like: %.1f, Order: %.1f, CarryOver: %.1f",
            WEIGHT_VIEW, WEIGHT_LIKE, WEIGHT_ORDER, WEIGHT_CARRY_OVER);
    }
}
