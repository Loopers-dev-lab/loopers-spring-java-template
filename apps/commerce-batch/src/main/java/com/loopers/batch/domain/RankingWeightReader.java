package com.loopers.batch.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis에서 랭킹 가중치를 읽어오는 컴포넌트
 * commerce-streamer의 RankingWeight와 동일한 Redis Key를 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingWeightReader {

    private final RedisTemplate<String, String> redisTemplate;

    // commerce-streamer의 RankingWeight와 동일한 Key
    private static final String WEIGHT_KEY = "ranking:config:weights";

    // 기본 가중치 (commerce-streamer와 동일)
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
     * 총점 계산
     */
    public double calculateTotalScore(int totalLikes, int totalSales, int totalViews) {
        return (totalViews * getViewWeight()) + (totalLikes * getLikeWeight()) + (totalSales * getOrderWeight());
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
}
