package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingScore;
import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis ZSET 기반 랭킹 집계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingAggregator {

    private final RedisTemplate<String, String> redisTemplate;

    private static final Duration TTL_DAILY = Duration.ofDays(2);    // 일간 랭킹 TTL: 2일
    private static final Duration TTL_HOURLY = Duration.ofHours(48); // 시간별 랭킹 TTL: 48시간

    /**
     * 일간 랭킹에 조회 점수 증가
     */
    public void incrementViewScore(Long productId) {
        String key = RankingKey.dailyToday();
        double score = RankingScore.viewScore();
        incrementScore(key, productId, score, TTL_DAILY);

        // 실시간 랭킹에도 반영
        String hourlyKey = RankingKey.hourlyNow();
        incrementScore(hourlyKey, productId, score, TTL_HOURLY);
    }

    /**
     * 일간 랭킹에 좋아요 점수 증가
     */
    public void incrementLikeScore(Long productId) {
        String key = RankingKey.dailyToday();
        double score = RankingScore.likeScore();
        incrementScore(key, productId, score, TTL_DAILY);

        // 실시간 랭킹에도 반영
        String hourlyKey = RankingKey.hourlyNow();
        incrementScore(hourlyKey, productId, score, TTL_HOURLY);
    }

    /**
     * 일간 랭킹에 좋아요 점수 감소
     */
    public void decrementLikeScore(Long productId) {
        String key = RankingKey.dailyToday();
        double score = RankingScore.unlikeScore();
        incrementScore(key, productId, score, TTL_DAILY);

        // 실시간 랭킹에도 반영
        String hourlyKey = RankingKey.hourlyNow();
        incrementScore(hourlyKey, productId, score, TTL_HOURLY);
    }

    /**
     * 일간 랭킹에 주문 점수 증가
     */
    public void incrementOrderScore(Long productId, int price, int amount) {
        String key = RankingKey.dailyToday();
        double score = RankingScore.orderScore(price, amount);
        incrementScore(key, productId, score, TTL_DAILY);

        // 실시간 랭킹에도 반영
        String hourlyKey = RankingKey.hourlyNow();
        incrementScore(hourlyKey, productId, score, TTL_HOURLY);
    }

    /**
     * 배치 점수 증가 (성능 최적화)
     * - 동일 productId의 점수를 합산한 후 한 번에 반영
     */
    public void incrementScoresBatch(Map<Long, Double> scoreMap) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return;
        }

        String dailyKey = RankingKey.dailyToday();
        String hourlyKey = RankingKey.hourlyNow();

        // Pipeline으로 성능 최적화
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (Entry<Long, Double> entry : scoreMap.entrySet()) {
                String member = entry.getKey().toString();
                Double score = entry.getValue();

                // 일간 랭킹
                connection.zIncrBy(dailyKey.getBytes(), score, member.getBytes());
                // 실시간 랭킹
                connection.zIncrBy(hourlyKey.getBytes(), score, member.getBytes());
            }
            return null;
        });

        // TTL 설정
        redisTemplate.expire(dailyKey, TTL_DAILY);
        redisTemplate.expire(hourlyKey, TTL_HOURLY);

        log.info("📊 배치 랭킹 점수 반영 완료 - count: {}, dailyKey: {}, hourlyKey: {}",
            scoreMap.size(), dailyKey, hourlyKey);
    }

    /**
     * ZSET에 점수 증가 (ZINCRBY)
     */
    private void incrementScore(String key, Long productId, double score, Duration ttl) {
        String member = productId.toString();

        try {
            Double newScore = redisTemplate.opsForZSet().incrementScore(key, member, score);

            // TTL 설정 (키가 처음 생성될 때)
            redisTemplate.expire(key, ttl);

            log.debug("📈 랭킹 점수 증가 - key: {}, productId: {}, score: {:.2f}, newScore: {:.2f}",
                key, productId, score, newScore);
        } catch (Exception e) {
            log.error("❌ 랭킹 점수 증가 실패 - key: {}, productId: {}, score: {}",
                key, productId, score, e);
        }
    }
}
