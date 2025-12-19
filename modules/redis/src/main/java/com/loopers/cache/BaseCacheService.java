package com.loopers.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기본 캐시 서비스
 * <p>
 * Redis 캐시의 공통 연산을 제공합니다.
 * 캐시 실패 시 로깅만 하고 서비스는 계속 동작합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BaseCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheKeyGenerator cacheKeyGenerator;

    // ========== 기본 캐시 연산 ==========

    /**
     * 캐시 저장
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, timeUnit);
            log.debug("캐시 저장 - key: {}", key);
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (JSON 직렬화) - key: {}", key);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - key: {}, error: {}", key, e.getMessage());
        }
    }

    /**
     * 캐시 조회
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 미스 - key: {}", key);
                return Optional.empty();
            }

            T result = objectMapper.readValue(value, clazz);
            log.debug("캐시 히트 - key: {}", key);

            return Optional.of(result);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - key: {}, error: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 캐시 삭제
     */
    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (deleted) {
                log.debug("캐시 삭제 - key: {}", key);
            }
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 - key: {}, error: {}", key, e.getMessage());
        }
    }

    /**
     * 패턴 기반 캐시 삭제
     */
    public void deleteByPattern(String pattern) {
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();

            Set<String> keys = new HashSet<>();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("패턴 캐시 삭제 - pattern: {}, count: {}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.warn("패턴 캐시 삭제 실패 - pattern: {}, error: {}", pattern, e.getMessage());
        }
    }

    // ========== 상품 캐시 무효화 ==========

    /**
     * 특정 상품의 캐시 무효화
     */
    public void evictProductCache(Long productId) {
        try {
            String productKey = cacheKeyGenerator.generateProductDetailKey(productId);
            delete(productKey);
            log.debug("상품 캐시 무효화 - productId: {}", productId);
        } catch (Exception e) {
            log.warn("상품 캐시 무효화 실패 - productId: {}", productId, e);
        }
    }

    /**
     * 상품 목록 관련 캐시 무효화
     */
    public void evictProductListCaches() {
        try {
            // 인기 상품 목록 캐시 삭제
            delete(cacheKeyGenerator.generatePopularProductsKey());

            // 상품 목록 관련 캐시들 패턴 매칭으로 삭제
            deleteByPattern(cacheKeyGenerator.generateProductListPattern());

            log.debug("상품 목록 캐시 무효화 완료");
        } catch (Exception e) {
            log.warn("상품 목록 캐시 무효화 실패", e);
        }
    }

    /**
     * 판매량 변화 시 캐시 무효화
     */
    public void onSalesCountChanged(Long productId) {
        evictProductCache(productId);
        evictProductListCaches();
        log.debug("판매량 변화로 캐시 무효화 - productId: {}", productId);
    }

    // ========== 브랜드 캐시 무효화 ==========

    /**
     * 특정 브랜드의 상품 목록 캐시 무효화
     */
    public void evictBrandProductListCache(Long brandId) {
        try {
            String pattern = cacheKeyGenerator.generateProductListPatternByBrand(brandId);
            deleteByPattern(pattern);
            log.debug("브랜드 상품 목록 캐시 무효화 - brandId: {}", brandId);
        } catch (Exception e) {
            log.warn("브랜드 상품 목록 캐시 무효화 실패 - brandId: {}", brandId, e);
        }
    }

    // ========== 상품 재고 캐시 갱신 ==========

    /**
     * 상품 상세 캐시의 재고 정보만 업데이트
     * 캐시가 존재하는 경우에만 업데이트하고, 없으면 스킵
     */
    public void updateProductStock(Long productId, Integer newStock) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            
            // 기존 캐시 조회
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue == null) {
                log.debug("상품 상세 캐시 없음 - 재고 갱신 스킵: productId={}", productId);
                return;
            }
            
            // JSON 파싱하여 재고 정보 업데이트
            try {
                // 기존 캐시 데이터를 Map으로 파싱
                @SuppressWarnings("unchecked")
                Map<String, Object> productData = objectMapper.readValue(cachedValue, java.util.Map.class);
                
                // 재고 정보 업데이트
                productData.put("stock", newStock);
                productData.put("isInStock", newStock > 0);
                
                // 업데이트된 데이터를 다시 JSON으로 변환하여 저장
                String updatedValue = objectMapper.writeValueAsString(productData);
                
                // 기존 TTL 유지하면서 업데이트
                long ttl = redisTemplate.getExpire(key);
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(key, updatedValue, ttl, TimeUnit.SECONDS);
                } else {
                    // TTL이 없거나 만료된 경우 기본 30분으로 설정
                    redisTemplate.opsForValue().set(key, updatedValue, 30, TimeUnit.MINUTES);
                }
                
                log.debug("상품 재고 캐시 갱신 완료: productId={}, newStock={}", productId, newStock);
                
            } catch (JsonProcessingException e) {
                log.warn("상품 재고 캐시 갱신 실패 (JSON 처리 오류) - 캐시 삭제: productId={}", productId, e);
                // JSON 파싱 실패 시 캐시 삭제
                delete(key);
            }
            
        } catch (Exception e) {
            log.warn("상품 재고 캐시 갱신 실패: productId={}, error: {}", productId, e.getMessage());
        }
    }
}
