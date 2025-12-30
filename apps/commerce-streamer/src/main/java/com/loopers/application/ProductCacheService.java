package com.loopers.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    
    // commerce-api에서 사용하는 캐시 키 패턴
    private static final String PRODUCT_INFO_KEY_PREFIX = "product:info:";
    private static final String PRODUCT_STAT_KEY_PREFIX = "product:stat:";
    private static final String PRODUCT_LIST_KEY_PREFIX = "products::list::";

    /**
     * 상품 캐시 삭제
     * commerce-api에서 사용하는 모든 캐시 키를 삭제합니다:
     * - product:info:{productId} - 상품 정보 캐시
     * - product:stat:{productId} - 상품 통계 캐시 (좋아요 수 등)
     * - products::list::* - 목록 캐시 (상품 변경 시 모든 목록 캐시 무효화)
     */
    public void evictProductCache(Long productId) {
        int deletedCount = 0;
        
        // 1. 상품 정보 캐시 삭제
        String infoKey = PRODUCT_INFO_KEY_PREFIX + productId;
        if (Boolean.TRUE.equals(redisTemplate.delete(infoKey))) {
            deletedCount++;
            log.debug("Deleted product info cache: {}", infoKey);
        }
        
        // 2. 상품 통계 캐시 삭제
        String statKey = PRODUCT_STAT_KEY_PREFIX + productId;
        if (Boolean.TRUE.equals(redisTemplate.delete(statKey))) {
            deletedCount++;
            log.debug("Deleted product stat cache: {}", statKey);
        }
        
        // 3. 목록 캐시 삭제 (패턴 매칭)
        int listCacheDeleted = evictListCaches();
        deletedCount += listCacheDeleted;
        
        if (deletedCount > 0) {
            log.info("Evicted product cache for productId: {} (deleted {} keys)", productId, deletedCount);
        } else {
            log.debug("No cache found for productId: {}", productId);
        }
    }
    
    /**
     * 목록 캐시 삭제 (products::list::* 패턴)
     * SCAN을 사용하여 안전하게 삭제합니다.
     */
    private int evictListCaches() {
        int deletedCount = 0;
        
        try {
            // SCAN을 사용하여 패턴 매칭 키 찾기
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(PRODUCT_LIST_KEY_PREFIX + "*")
                    .count(100) // 한 번에 최대 100개씩 스캔
                    .build();
            
            try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    if (key != null && Boolean.TRUE.equals(redisTemplate.delete(key))) {
                        deletedCount++;
                        log.debug("Deleted list cache: {}", key);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to evict list caches using SCAN, falling back to KEYS command", e);
            // SCAN 실패 시 KEYS 사용 (프로덕션에서는 주의 필요)
            Set<String> keys = redisTemplate.keys(PRODUCT_LIST_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                deletedCount = deleted != null ? deleted.intValue() : 0;
                log.debug("Deleted {} list caches using KEYS command", deletedCount);
            }
        }
        
        return deletedCount;
    }
}
