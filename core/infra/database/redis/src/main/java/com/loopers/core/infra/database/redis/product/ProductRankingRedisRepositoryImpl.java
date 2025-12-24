package com.loopers.core.infra.database.redis.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductRankingRedisRepositoryImpl implements ProductRankingRedisRepository {

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final long TTL_DAYS = 2;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void increaseDaily(String productId, LocalDate date, Double score) {
        String key = RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key, productId, score);
        redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));
    }

    @Override
    public Set<TypedTuple<String>> getTopProductsWithScores(LocalDate date, long start, long stop) {
        String key = RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, stop);
    }

    @Override
    public Long countRankings(LocalDate date) {
        String key = RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
        return redisTemplate.opsForZSet().zCard(key);
    }

    @Override
    public Double getRankScore(LocalDate date, String productId) {
        String key = RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
        return redisTemplate.opsForZSet().score(key, productId);
    }

    @Override
    public Long getRankingPosition(LocalDate date, String productId) {
        String key = RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
        Long rank = redisTemplate.opsForZSet().reverseRank(key, productId);
        if (rank == null) {
            return null;
        }
        return rank + 1;
    }
}
