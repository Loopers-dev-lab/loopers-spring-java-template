package com.loopers.domain.product;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.infrastructure.cache.CacheKeyGenerator;
import com.loopers.infrastructure.cache.CacheStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 서비스 구현체
 * Hot/Warm/Cold 전략별로 Redis 캐싱 관리
 * 캐시 실패 시 로깅만 하고 서비스는 계속 동작
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final ObjectMapper objectMapper;

    // TTL 상수 - Hot/Warm/Cold 전략별 차별화
    private static final long HOT_DATA_TTL = 30; // Hot: 30분 (배치 갱신)
    private static final long WARM_DATA_TTL = 10; // Warm: 10분 (Cache-Aside)
    private static final TimeUnit TTL_UNIT = TimeUnit.MINUTES;

    // ========== Hot: 상품 상세 (배치 갱신, TTL 60분) ==========

    public void cacheProductDetail(Long productId, ProductDetailInfo productDetail) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = objectMapper.writeValueAsString(productDetail);

            redisTemplate.opsForValue().set(key, value, HOT_DATA_TTL, TTL_UNIT);

            log.debug("상품 상세 캐시 저장 - productId: {}", productId);
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 저장 실패 (JSON 직렬화 오류) - productId: {}, error: {}",
                    productId, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 상세 캐시 저장 실패 - productId: {}, error: {}",
                    productId, e.getMessage());
        }
    }


    public void batchCacheProductDetails(java.util.List<ProductDetailInfo> productDetails) {
        if (productDetails == null || productDetails.isEmpty()) {
            log.debug("배치 캐시 저장 대상 없음");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (ProductDetailInfo productDetail : productDetails) {
            try {
                cacheProductDetail(productDetail.id(), productDetail);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("배치 캐시 저장 실패 - productId: {}", productDetail.id());
            }
        }

        log.info("배치 캐시 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }


    public Optional<ProductDetailInfo> getProductDetailFromCache(Long productId) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 미스 - productId: {}", productId);
                return Optional.empty();
            }

            ProductDetailInfo productDetail = objectMapper.readValue(value, ProductDetailInfo.class);
            log.debug("캐시 히트 - productId: {}", productId);

            return Optional.of(productDetail);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - productId: {}", productId);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - productId: {}", productId);
            return Optional.empty();
        }
    }


    public void evictProductDetail(Long productId) {
        try {
            String key = cacheKeyGenerator.generateProductDetailKey(productId);
            redisTemplate.delete(key);

            log.debug("캐시 삭제 - productId: {}", productId);
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 - productId: {}", productId);
        }
    }

    // ========== Hot/Warm: 상품 ID 리스트 (Hot: 60분, Warm: 10분) ==========


    public void cacheProductIds(CacheStrategy strategy, Long brandId,
                                Pageable pageable,
                                List<Long> productIds) {
        try {
            String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
            String value = objectMapper.writeValueAsString(productIds);

            long ttl = strategy == CacheStrategy.HOT ? HOT_DATA_TTL : WARM_DATA_TTL;
            redisTemplate.opsForValue().set(key, value, ttl, TTL_UNIT);

            log.debug("상품 ID 리스트 캐시 저장 - strategy: {}, brandId: {}", strategy, brandId);
        } catch (JsonProcessingException e) {
            log.warn("상품 ID 리스트 캐시 저장 실패 (JSON 직렬화) - strategy: {}", strategy);
        } catch (Exception e) {
            log.warn("상품 ID 리스트 캐시 저장 실패 - strategy: {}", strategy);
        }
    }


    public Optional<List<Long>> getProductIdsFromCache(CacheStrategy strategy, Long brandId,
                                                       Pageable pageable) {
        try {
            String key = cacheKeyGenerator.generateProductIdsKey(strategy, brandId, pageable);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("캐시 미스 - strategy: {}, brandId: {}", strategy, brandId);
                return Optional.empty();
            }

            List<Long> productIds = objectMapper.readValue(value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));

            log.debug("캐시 히트 - strategy: {}, brandId: {}", strategy, brandId);

            return Optional.of(productIds);
        } catch (JsonProcessingException e) {
            log.warn("캐시 조회 실패 (JSON 역직렬화) - strategy: {}", strategy);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("캐시 조회 실패 - strategy: {}", strategy);
            return Optional.empty();
        }
    }


    public void evictProductIdsByStrategy(CacheStrategy strategy) {
        try {
            String pattern = cacheKeyGenerator.generateProductIdsPattern(strategy);
            deleteByPattern(pattern);

            log.debug("전략별 캐시 삭제 - strategy: {}", strategy);
        } catch (Exception e) {
            log.warn("전략별 캐시 삭제 실패 - strategy: {}", strategy);
        }
    }

    // ========== Cold: 전체 Page 객체 (레거시, TTL 5분) ==========


    public void evictProductListByBrand(Long brandId) {
        try {
            String pattern = cacheKeyGenerator.generateProductListPatternByBrand(brandId);
            deleteByPattern(pattern);

            log.debug("브랜드 목록 캐시 삭제 - brandId: {}", brandId);
        } catch (Exception e) {
            log.warn("브랜드 목록 캐시 삭제 실패 - brandId: {}", brandId);
        }
    }

    // ========== 범용 캐시 연산 ==========


    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, timeUnit);

            log.debug("캐시 저장 - key: {}", key);
        } catch (JsonProcessingException e) {
            log.warn("캐시 저장 실패 (JSON 직렬화) - key: {}", key);
        } catch (Exception e) {
            log.warn("캐시 저장 실패 - key: {}", key);
        }
    }


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
            log.warn("캐시 조회 실패 - key: {}", key);
            return Optional.empty();
        }
    }

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
            log.warn("패턴 캐시 삭제 실패 - pattern: {}", pattern, e);
        }
    }

    // ========== 세밀한 캐시 무효화 (Incremental Invalidation) ==========


    public void evictProductCaches(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            log.debug("무효화 대상 없음");
            return;
        }

        log.info("상품 캐시 무효화 - 대상: {}개", productIds.size());

        int deletedCount = 0;

        for (Long productId : productIds) {
            try {
                evictProductDetail(productId);
                deletedCount++;
            } catch (Exception e) {
                log.warn("상품 캐시 무효화 실패 - productId: {}", productId);
            }
        }

        log.info("상품 캐시 무효화 완료 - 삭제: {}개", deletedCount);
    }


    public void evictBrandCaches(Set<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            log.debug("무효화 대상 없음");
            return;
        }

        log.info("브랜드 캐시 무효화 - 대상: {}개", brandIds.size());

        int deletedKeyCount = 0;

        for (Long brandId : brandIds) {
            try {
                // Hot 캐시 삭제
                String hotPattern = String.format("product:ids:hot:brand:%d:*", brandId);
                Set<String> hotKeys = new HashSet<>();
                try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions().match(hotPattern).count(100).build())) {
                    cursor.forEachRemaining(hotKeys::add);
                }
                if (!hotKeys.isEmpty()) {
                    redisTemplate.delete(hotKeys);
                    deletedKeyCount += hotKeys.size();
                }

                // Warm 캐시 삭제
                String warmPattern = String.format("product:ids:warm:brand:%d:*", brandId);
                Set<String> warmKeys = new HashSet<>();
                try (Cursor<String> cursor = redisTemplate.scan(
                        ScanOptions.scanOptions().match(warmPattern).count(100).build())) {
                    cursor.forEachRemaining(warmKeys::add);
                }
                if (!warmKeys.isEmpty()) {
                    redisTemplate.delete(warmKeys);
                    deletedKeyCount += warmKeys.size();
                }

                // 레거시 캐시 삭제
                evictProductListByBrand(brandId);

            } catch (Exception e) {
                log.warn("브랜드 캐시 무효화 실패 - brandId: {}", brandId);
            }
        }

        log.info("브랜드 캐시 무효화 완료 - 삭제 키: {}개", deletedKeyCount);
    }


    public void evictCachesAfterMVSync(Set<Long> changedProductIds, Set<Long> affectedBrandIds) {
        log.info("MV 동기화 후 캐시 무효화 - 변경상품: {}개, 브랜드: {}개",
                changedProductIds.size(), affectedBrandIds.size());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 변경된 상품 상세 캐시 무효화
            evictProductCaches(changedProductIds);

            // 2. 영향받은 브랜드 목록 캐시 무효화
            evictBrandCaches(affectedBrandIds);

            // 3. 전체 상품 목록 캐시 무효화
            if (!changedProductIds.isEmpty()) {
                deleteByPattern("product:ids:hot:brand:null:*");
                deleteByPattern("product:ids:warm:brand:null:*");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("MV 동기화 캐시 무효화 완료 - {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("MV 동기화 캐시 무효화 실패 - {}ms", duration, e);
        }
    }

    /**
     * 캐시 전략 결정
     */
    public CacheStrategy determineCacheStrategy(ProductSearchFilter filter) {
        if (filter.pageable().getPageNumber() == 0)
            return CacheStrategy.HOT;

        if (filter.pageable().getPageNumber() > 2)
            return CacheStrategy.COLD;

        if (filter.productName() != null && !filter.productName().trim().isEmpty()) {
            log.debug("Cold 전략 선택 - 상품명 검색: {}", filter.productName());
            return CacheStrategy.COLD;
        }

        if (filter.brandId() != null) {
            if (isPopularitySort(filter.pageable())) {
                log.debug("Hot 전략 선택 - 브랜드: {}, 인기순 정렬", filter.brandId());
                return CacheStrategy.HOT;
            }
            log.debug("Warm 전략 선택 - 브랜드: {}", filter.brandId());
            return CacheStrategy.WARM;
        }

        log.debug("Warm 전략 선택 - 전체 목록");
        return CacheStrategy.WARM;
    }

    /**
     * 인기순 정렬 여부 확인
     */
    private boolean isPopularitySort(Pageable pageable) {
        return pageable.getSort().stream()
                .anyMatch(order -> "likeCount".equals(order.getProperty()) && order.isDescending());
    }
}
