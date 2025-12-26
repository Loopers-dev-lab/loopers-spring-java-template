package com.loopers.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 상품별 좋아요 카운터 캐시
 * Redis INCR/DECR 사용: product:like:count:{productId}
 * 주기적으로 DB와 동기화
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ProductLikeCountCache {

    private final RedisTemplate<String, Object> cacheRedisTemplate;

    /**
     * 좋아요 카운트 증가
     */
    public void increment(Long productId) {
        try {
            String key = CacheKeyGenerator.productLikeCountKey(productId);
            cacheRedisTemplate.opsForValue().increment(key);
            log.debug("[ProductLikeCountCache] increment productId={}", productId);
        } catch (Exception e) {
            log.warn("[ProductLikeCountCache] increment failed, error={}", e.getMessage());
        }
    }

    /**
     * 좋아요 카운트 감소
     */
    public void decrement(Long productId) {
        try {
            String key = CacheKeyGenerator.productLikeCountKey(productId);
            Long newValue = cacheRedisTemplate.opsForValue().decrement(key);

            // 음수 방지
            if (newValue != null && newValue < 0) {
                cacheRedisTemplate.opsForValue().set(key, 0);
            }

            log.debug("[ProductLikeCountCache] decrement productId={}", productId);
        } catch (Exception e) {
            log.warn("[ProductLikeCountCache] decrement failed, error={}", e.getMessage());
        }
    }

    /**
     * 현재 카운트 조회
     */
    public Long get(Long productId) {
        try {
            String key = CacheKeyGenerator.productLikeCountKey(productId);
            Object value = cacheRedisTemplate.opsForValue().get(key);
            return toLongOrNull(value);
        } catch (Exception e) {
            log.warn("[ProductLikeCountCache] get failed, error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 카운트 설정 (초기화/동기화용)
     */
    public void set(Long productId, Long count) {
        try {
            String key = CacheKeyGenerator.productLikeCountKey(productId);
            cacheRedisTemplate.opsForValue().set(key, count);
            log.debug("[ProductLikeCountCache] set productId={}, count={}", productId, count);
        } catch (Exception e) {
            log.warn("[ProductLikeCountCache] set failed, error={}", e.getMessage());
        }
    }

    /**
     * 모든 카운터 조회 (DB 동기화용)
     * @return Map<productId, count>
     */
    public Map<Long, Long> getAllCounts() {
        Map<Long, Long> result = new HashMap<>();

        try {
            String pattern = CacheKeyGenerator.productLikeCountKeyPattern();
            Set<String> keys = cacheRedisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return result;
            }

            for (String key : keys) {
                try {
                    // key에서 productId 추출
                    Long productId = extractProductIdFromKey(key);
                    Object value = cacheRedisTemplate.opsForValue().get(key);
                    Long count = toLongOrNull(value);

                    if (count != null && productId != null) {
                        result.put(productId, count);
                    }
                } catch (Exception e) {
                    log.warn("[ProductLikeCountCache] Failed to process key={}, error={}", key, e.getMessage());
                }
            }

            log.debug("[ProductLikeCountCache] getAllCounts size={}", result.size());
        } catch (Exception e) {
            log.error("[ProductLikeCountCache] getAllCounts failed, error={}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 캐시 삭제
     */
    public void delete(Long productId) {
        try {
            String key = CacheKeyGenerator.productLikeCountKey(productId);
            cacheRedisTemplate.delete(key);
            log.debug("[ProductLikeCountCache] delete productId={}", productId);
        } catch (Exception e) {
            log.warn("[ProductLikeCountCache] delete failed, error={}", e.getMessage());
        }
    }

    /**
     * key에서 productId 추출
     * product:like:count:v1:123 → 123
     */
    private Long extractProductIdFromKey(String key) {
        try {
            String[] parts = key.split(":");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("[ProductLikeCountCache] extractProductIdFromKey failed, key={}", key);
            return null;
        }
    }

    /**
     * Redis 값을 Long으로 변환 (변환 실패 시 null 반환)
     */
    private Long toLongOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("[ProductLikeCountCache] Failed to parse String to Long: {}", value);
                return null;
            }
        }
        log.warn("[ProductLikeCountCache] Unsupported value type: {}", value.getClass().getSimpleName());
        return null;
    }
}
