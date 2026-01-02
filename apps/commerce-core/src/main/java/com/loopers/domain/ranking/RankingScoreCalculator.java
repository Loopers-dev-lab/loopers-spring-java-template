package com.loopers.domain.ranking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 랭킹 점수 계산기
 * <p>
 * 이벤트 타입별 가중치를 적용하여 랭킹 점수를 계산합니다:
 * - 조회(ProductViewed): Weight = 0.1, Score = 1
 * - 좋아요(ProductLiked): Weight = 0.2, Score = 1
 * - 주문(OrderPaid): Weight = 0.7, Score = price * amount
 */
@Slf4j
@Component
public class RankingScoreCalculator {

    // 가중치 상수
    private static final double WEIGHT_VIEW = 0.1;
    private static final double WEIGHT_LIKE = 0.2;
    private static final double WEIGHT_ORDER = 0.7;

    /**
     * 조회 이벤트 점수 계산
     *
     * @return Weight(0.1) * Score(1) = 0.1
     */
    public double calculateViewScore() {
        return WEIGHT_VIEW * 1.0;
    }

    /**
     * 좋아요 이벤트 점수 계산
     *
     * @return Weight(0.2) * Score(1) = 0.2
     */
    public double calculateLikeScore() {
        return WEIGHT_LIKE * 1.0;
    }

    /**
     * 주문 이벤트 점수 계산
     *
     * @param price  상품 가격
     * @param amount 주문 수량
     * @return Weight(0.7) * Score(price * amount)
     */
    public double calculateOrderScore(Long price, Integer amount) {
        if (price == null || price <= 0 || amount == null || amount <= 0) {
            log.warn("Invalid price or amount for order score calculation: price={}, amount={}", price, amount);
            return 0.0;
        }
        // 정규화를 위해 log 적용 고려 (선택사항)
        // return WEIGHT_ORDER * Math.log(1 + price * amount);
        return WEIGHT_ORDER * price * amount;
    }

    /**
     * 이벤트 타입별 점수 계산
     *
     * @param eventType 이벤트 타입 (ProductViewed, ProductLiked, OrderPaid 등)
     * @param eventData 이벤트 데이터 (price, amount 등 포함)
     * @return 계산된 점수
     */
    public double calculateScore(String eventType, Map<String, Object> eventData) {
        switch (eventType) {
            case "ProductViewed":
                return calculateViewScore();
            case "ProductLiked":
                return calculateLikeScore();
            case "OrderPaid":
                Long price = extractPrice(eventData);
                Integer amount = extractAmount(eventData);
                return calculateOrderScore(price, amount);
            default:
                log.debug("Unknown event type for ranking score: {}", eventType);
                return 0.0;
        }
    }

    private Long extractPrice(Map<String, Object> eventData) {
        if (eventData == null) {
            return null;
        }
        Object priceObj = eventData.get("price");
        if (priceObj == null) {
            return null;
        }
        if (priceObj instanceof Number) {
            return ((Number) priceObj).longValue();
        }
        try {
            return Long.parseLong(priceObj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse price: {}", priceObj, e);
            return null;
        }
    }

    private Integer extractAmount(Map<String, Object> eventData) {
        if (eventData == null) {
            return null;
        }
        Object amountObj = eventData.get("amount");
        if (amountObj == null) {
            return null;
        }
        if (amountObj instanceof Number) {
            return ((Number) amountObj).intValue();
        }
        try {
            return Integer.parseInt(amountObj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountObj, e);
            return null;
        }
    }
}

