package com.loopers.application.product.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, Object> productCacheTemplate;
    private static final String CACHE_PREFIX = "product:detail:";

    /**
     * 상품 캐시 무효화
     */
    public void evictProductCache(Long productId) {
        String cacheKey = CACHE_PREFIX + productId;
        try {
            // cache invalidate 처리
            Boolean deleted = productCacheTemplate.delete(cacheKey);

            if (deleted) {
                log.info("상품 캐시 무효화 성공 - productId: {}", productId);
            } else {
                log.warn("상품 캐시가 존재하지 않음 - productId: {}", productId);
            }
        } catch (Exception e) {
            log.error("상품 캐시 무효화 실패 - productId: {}", productId, e);
            // TTL이 지나면 자동으로 갱신됨
        }
    }
}
