package com.loopers.infrastructure.product;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheRefresher {

    private static final String DETAIL_PREFIX = "product:detail:";
    private static final String LIST_PREFIX = "product:list:";

    private final RedisTemplate<String, String> redisTemplate;

    public void evict(Long productId, Long brandId) {
        if (productId == null) {
            return;
        }
        String detailKey = DETAIL_PREFIX + productId;
        redisTemplate.delete(detailKey);
        log.debug("Evicted product detail cache key={}", detailKey);

        evictPattern(LIST_PREFIX + "all:*");

        if (brandId != null) {
            evictPattern(LIST_PREFIX + brandId + ":*");
        }
    }

    private void evictPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        redisTemplate.delete(keys);
        log.debug("Evicted {} keys for pattern={}", keys.size(), pattern);
    }
}
