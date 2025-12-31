package com.loopers.application;

import com.loopers.cache.CacheKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingSyncScheduler {

    private final CacheKeyService cacheKeyService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final double YESTERDAY_WEIGHT = 0.5;
    private static final double TODAY_WEIGHT = 1.0;

    /**
     * 5분마다 재집계
     * 어제 0.5, 오늘 1 비율로 반영
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void recalculateRanking() {

        String todayKey = cacheKeyService.rankingKey(LocalDate.now());
        String yesterdayKey = cacheKeyService.rankingKey(LocalDate.now().minusDays(1));

        // 1. 오늘 / 어제 ZSET 전체 읽기
        Set<ZSetOperations.TypedTuple<String>> todaySet =
                redisTemplate.opsForZSet().rangeWithScores(todayKey, 0, -1);
        Set<ZSetOperations.TypedTuple<String>> yesterdaySet =
                redisTemplate.opsForZSet().rangeWithScores(yesterdayKey, 0, -1);

        if (todaySet == null) todaySet = Collections.emptySet();
        if (yesterdaySet == null) yesterdaySet = Collections.emptySet();

        // 2. productId -> score 합산 맵
        Map<String, Double> scores = new HashMap<>();

        // 2-1. 어제 점수 먼저 반영 (weight 0.5)
        for (ZSetOperations.TypedTuple<String> tuple : yesterdaySet) {
            String productId = tuple.getValue();
            Double score = tuple.getScore();
            if (productId == null || score == null) continue;

            scores.put(productId, score * YESTERDAY_WEIGHT);
        }

        // 2-2. 오늘 점수 1.0 비율로 합산
        for (ZSetOperations.TypedTuple<String> tuple : todaySet) {
            String productId = tuple.getValue();
            Double score = tuple.getScore();
            if (productId == null || score == null) continue;

            double current = scores.getOrDefault(productId, 0.0);
            scores.put(productId, current + score * TODAY_WEIGHT);
        }

        // 3. 오늘 키를 초기화하고 새 점수로 다시 채우기
        redisTemplate.delete(todayKey);

        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            zset.add(todayKey, entry.getKey(), entry.getValue());
        }

        // 4. TTL 다시 설정 (2일)
        redisTemplate.expire(todayKey, Duration.ofDays(2));
    }
}
