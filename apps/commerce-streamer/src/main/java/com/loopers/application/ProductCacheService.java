package com.loopers.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "products::";

    public void evictProductCache(Long productId) {
        String key = CACHE_KEY_PREFIX + productId;
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Evicted product cache for productId: {}", productId);
        } else {
            log.debug("No cache found for productId: {} (key: {})", productId, key);
        }
    }
}
