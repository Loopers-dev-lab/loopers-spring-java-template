package com.loopers.core.infra.database.redis.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProductLikeRedisRepositoryImpl implements ProductLikeRedisRepository {

    private static final String LIKE_PRODUCT_CACHE_KEY_PREFIX = "like:product:";
    private static final String LIKE_USER_CACHE_KEY_PREFIX = "like:user:";

    private static final String UNLIKE_PRODUCT_CACHE_KEY_PREFIX = "unlike:product:";
    private static final String UNLIKE_USER_CACHE_KEY_PREFIX = "unlike:user:";

    private static final long TTL_DAYS = 1;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public void saveLike(String productId, String userId, Long timestamp) {
        redisTemplate.opsForZSet().add(LIKE_PRODUCT_CACHE_KEY_PREFIX + productId, userId, timestamp);
        redisTemplate.opsForZSet().add(LIKE_USER_CACHE_KEY_PREFIX + userId, productId, timestamp);

        redisTemplate.expire(LIKE_PRODUCT_CACHE_KEY_PREFIX + productId, Duration.ofDays(TTL_DAYS));
        redisTemplate.expire(LIKE_USER_CACHE_KEY_PREFIX + userId, Duration.ofDays(TTL_DAYS));
    }

    @Override
    @Transactional
    public void deleteLike(String productId, String userId) {
        redisTemplate.opsForZSet().remove(LIKE_PRODUCT_CACHE_KEY_PREFIX + productId, userId);
        redisTemplate.opsForZSet().remove(LIKE_USER_CACHE_KEY_PREFIX + userId, productId);
    }

    @Override
    public void saveUnlike(String productId, String userId, Long timestamp) {
        redisTemplate.opsForZSet().add(UNLIKE_PRODUCT_CACHE_KEY_PREFIX + productId, userId, timestamp);
        redisTemplate.opsForZSet().add(UNLIKE_USER_CACHE_KEY_PREFIX + userId, productId, timestamp);

        redisTemplate.expire(UNLIKE_PRODUCT_CACHE_KEY_PREFIX + productId, Duration.ofDays(TTL_DAYS));
        redisTemplate.expire(UNLIKE_USER_CACHE_KEY_PREFIX + userId, Duration.ofDays(TTL_DAYS));
    }

    @Override
    public void deleteUnlike(String productId, String userId) {
        redisTemplate.opsForZSet().remove(UNLIKE_PRODUCT_CACHE_KEY_PREFIX + productId, userId);
        redisTemplate.opsForZSet().remove(UNLIKE_USER_CACHE_KEY_PREFIX + userId, productId);
    }
}
