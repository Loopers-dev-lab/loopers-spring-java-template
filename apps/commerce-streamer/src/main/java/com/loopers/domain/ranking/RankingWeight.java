package com.loopers.domain.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingWeight {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String WEIGHT_KEY = "ranking:config:weights";

    // 기본 가중치
    private static final double DEFAULT_VIEW_WEIGHT = 0.1;
    private static final double DEFAULT_LIKE_WEIGHT = 0.2;
    private static final double DEFAULT_ORDER_WEIGHT = 0.7;

    public double getViewWeight() {
        return getWeight("view", DEFAULT_VIEW_WEIGHT);
    }

    public double getLikeWeight() {
        return getWeight("like", DEFAULT_LIKE_WEIGHT);
    }

    public double getOrderWeight() {
        return getWeight("order", DEFAULT_ORDER_WEIGHT);
    }

    /**
     * 조회 이벤트 점수 계산
     */
    public double calculateViewScore() {
        return getViewWeight() * 1.0;
    }

    /**
     * 좋아요 이벤트 점수 계산
     */
    public double calculateLikeScore(boolean isLike) {
        return getLikeWeight() * (isLike ? 1.0 : -1.0);
    }

    /**
     * 주문 이벤트 점수 계산 (수량 기반)
     */
    public double calculateOrderScore(int quantity) {
        return getOrderWeight() * quantity;
    }

    /**
     * 주문 이벤트 점수 계산 (금액 기반, log 스케일)
     */
    public double calculateOrderScoreWithAmount(long amount) {
        if (amount <= 0) {
            return 0;
        }
        return getOrderWeight() * Math.log10(amount);
    }

    public void updateViewWeight(double weight) {
        updateWeight("view", weight);
    }

    public void updateLikeWeight(double weight) {
        updateWeight("like", weight);
    }

    public void updateOrderWeight(double weight) {
        updateWeight("order", weight);
    }

    /**
     * 모든 가중치 일괄 업데이트
     */
    public void updateAllWeights(double viewWeight, double likeWeight, double orderWeight) {
        try {
            redisTemplate.opsForHash().put(WEIGHT_KEY, "view", String.valueOf(viewWeight));
            redisTemplate.opsForHash().put(WEIGHT_KEY, "like", String.valueOf(likeWeight));
            redisTemplate.opsForHash().put(WEIGHT_KEY, "order", String.valueOf(orderWeight));
            log.info("랭킹 가중치 일괄 업데이트: view={}, like={}, order={}",
                    viewWeight, likeWeight, orderWeight);
        } catch (Exception e) {
            log.error("랭킹 가중치 일괄 업데이트 실패: view={}, like={}, order={}, error={}",
                    viewWeight, likeWeight, orderWeight, e.getMessage(), e);
        }
    }

    /**
     * Redis에서 가중치 삭제 (기본값으로 복원)
     */
    public void resetToDefault() {
        redisTemplate.delete(WEIGHT_KEY);
        log.info("랭킹 가중치 초기화: 기본값으로 복원");
    }

    private double getWeight(String field, double defaultValue) {
        try {
            Object value = redisTemplate.opsForHash().get(WEIGHT_KEY, field);
            if (value != null) {
                return Double.parseDouble(value.toString());
            }
        } catch (Exception e) {
            log.warn("가중치 조회 실패, 기본값 사용: field={}, default={}", field, defaultValue, e);
        }
        return defaultValue;
    }

    private void updateWeight(String field, double weight) {
        try {
            redisTemplate.opsForHash().put(WEIGHT_KEY, field, String.valueOf(weight));
            log.info("랭킹 가중치 업데이트: {}={}", field, weight);
        } catch (Exception e) {
            log.error("가중치 업데이트 실패: field={}, weight={}", field, weight, e);
        }
    }
}
