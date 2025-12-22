package com.loopers.core.infra.database.redis.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ProductRankingRedisRepositoryImpl implements ProductRankingRedisRepository {

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final long TTL_DAYS = 2;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void increaseDaily(String productId, LocalDateTime date, Double score) {
        String key = RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key, productId, score);
        redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));
    }
}
