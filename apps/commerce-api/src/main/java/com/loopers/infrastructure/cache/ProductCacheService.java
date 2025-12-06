package com.loopers.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.interfaces.api.product.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 상품 캐시 서비스
 * <p>
 * Redis를 사용하여 API 응답을 캐싱합니다.
 * Cache-Aside 패턴을 사용합니다.
 */
@Service
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;

    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_KEY_PREFIX = "product:list:";

    public ProductCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${cache.product.ttl-minutes:5}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * 상품 상세 캐시 조회
     * <p>
     * Cache-Aside 패턴의 "읽기" 부분
     */
    public Optional<ProductDetailCache> getProductDetail(Long productId) {
        String key = PRODUCT_DETAIL_KEY_PREFIX + productId;

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(cached, ProductDetailCache.class));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 상품 상세 캐시 저장
     * <p>
     * Cache-Aside 패턴의 "쓰기" 부분
     */
    public void setProductDetail(Long productId, ProductDetailCache cache) {
        String key = PRODUCT_DETAIL_KEY_PREFIX + productId;

        try {
            String value = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(key, value, cacheTtl);

        } catch (Exception e) {
            // 캐시 저장 실패는 무시 (다음 요청에서 DB 조회)
        }
    }

    /**
     * 상품 상세 캐시 삭제 (무효화)
     * <p>
     * 상품 정보가 변경되었을 때 캐시를 즉시 삭제합니다.
     */
    public void deleteProductDetail(Long productId) {
        String key = PRODUCT_DETAIL_KEY_PREFIX + productId;

        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            // 캐시 삭제 실패는 무시
        }
    }

    /**
     * 모든 상품 목록 캐시 무효화
     * <p>
     * totalLikes 같은 필드가 변경되었을 때 모든 목록 캐시를 무효화합니다.
     * 좋아요 추가/제거 시 호출됨.
     */
    public void invalidateProductListCaches() {
        try {
            Set<String> listKeys = redisTemplate.keys(PRODUCT_LIST_KEY_PREFIX + "*");
            if (listKeys != null && !listKeys.isEmpty()) {
                redisTemplate.delete(listKeys);
            }
        } catch (Exception e) {
            // 캐시 무효화 실패는 무시
        }
    }

    /**
     * 상품 목록 전체 응답 캐시 조회 (Cache-Aside 패턴 최적화)
     */
    public Optional<ProductDto.ProductListResponse> getProductListResponse(
            Long brandId, String sort, int page, int size) {
        String key = buildProductListResponseKey(brandId, sort, page, size);

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }

            return Optional.of(
                    objectMapper.readValue(cached, ProductDto.ProductListResponse.class)
            );

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 상품 목록 전체 응답 캐시 저장
     */
    public void setProductListResponse(
            Long brandId, String sort, int page, int size,
            ProductDto.ProductListResponse response) {
        String key = buildProductListResponseKey(brandId, sort, page, size);

        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, value, cacheTtl);

        } catch (Exception e) {
            // 캐시 저장 실패는 무시
        }
    }

    /**
     * 상품 목록 전체 응답 캐시 키 생성
     */
    private String buildProductListResponseKey(
            Long brandId, String sort, int page, int size) {
        String brandPart = brandId != null ? "brand:" + brandId : "all";
        return String.format("%sresponse:%s:%s:page:%d:size:%d",
                PRODUCT_LIST_KEY_PREFIX, brandPart, sort, page, size);
    }

    /**
     * 모든 상품 캐시 삭제
     * <p>
     * 테스트 격리를 위해 사용
     * product:detail:*, product:list:* 패턴의 모든 키 삭제
     */
    public void clearAllProductCache() {
        try {
            Set<String> detailKeys = redisTemplate.keys(PRODUCT_DETAIL_KEY_PREFIX + "*");
            if (detailKeys != null && !detailKeys.isEmpty()) {
                redisTemplate.delete(detailKeys);
            }

            Set<String> listKeys = redisTemplate.keys(PRODUCT_LIST_KEY_PREFIX + "*");
            if (listKeys != null && !listKeys.isEmpty()) {
                redisTemplate.delete(listKeys);
            }

        } catch (Exception e) {
            // 캐시 삭제 실패는 무시
        }
    }

    public static class ProductListCache {
        private List<Long> productIds;
        private long totalCount;

        public ProductListCache() {
        }

        public ProductListCache(List<Long> productIds, long totalCount) {
            this.productIds = productIds;
            this.totalCount = totalCount;
        }

        public List<Long> getProductIds() {
            return productIds;
        }

        public void setProductIds(List<Long> productIds) {
            this.productIds = productIds;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
    }
}
