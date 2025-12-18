package com.loopers.infrastructure.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 관리 서비스
 * 메트릭 변화 시 관련 캐시를 무효화
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String PRODUCT_LIST_CACHE_PREFIX = "product-list:";
    private static final String POPULAR_PRODUCTS_KEY = "popular-products";
    
    /**
     * 특정 상품의 캐시를 무효화
     */
    public void evictProductCache(Long productId) {
        try {
            String productKey = PRODUCT_CACHE_PREFIX + productId;
            Boolean deleted = redisTemplate.delete(productKey);
            
            if (deleted) {
                log.debug("Evicted product cache for productId: {}", productId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict product cache for productId: {}", productId, e);
        }
    }
    
    /**
     * 상품 목록 관련 캐시들을 무효화
     * 인기 상품, 추천 상품 등의 목록이 변경될 수 있음
     */
    public void evictProductListCaches() {
        try {
            // 인기 상품 목록 캐시 삭제
            redisTemplate.delete(POPULAR_PRODUCTS_KEY);
            
            // 상품 목록 관련 캐시들 패턴 매칭으로 삭제
            var keys = redisTemplate.keys(PRODUCT_LIST_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} product list cache entries", keys.size());
            }
        } catch (Exception e) {
            log.warn("Failed to evict product list caches", e);
        }
    }
    
    /**
     * 판매량 변화 시 호출 - 인기 상품 순위가 변경될 수 있음
     */
    public void onSalesCountChanged(Long productId) {
        evictProductCache(productId);
        evictProductListCaches();
        log.debug("Evicted caches due to sales count change for productId: {}", productId);
    }
    
    /**
     * 좋아요 수 변화 시 호출 - 상품 상세 정보 갱신 필요
     */
    public void onLikeCountChanged(Long productId) {
        evictProductCache(productId);
        log.debug("Evicted product cache due to like count change for productId: {}", productId);
    }
    
    /**
     * 조회수 변화 시 호출 - 일반적으로 캐시 무효화하지 않음 (성능상 이유)
     * 하지만 특정 임계값을 넘으면 인기 상품 목록 갱신
     */
    public void onViewCountChanged(Long productId, long newViewCount) {
        // 조회수가 특정 임계값(예: 1000의 배수)을 넘으면 인기 상품 목록 갱신
        if (newViewCount > 0 && newViewCount % 1000 == 0) {
            evictProductListCaches();
            log.debug("Evicted product list caches due to view milestone for productId: {} (views: {})", 
                     productId, newViewCount);
        }
    }
}
