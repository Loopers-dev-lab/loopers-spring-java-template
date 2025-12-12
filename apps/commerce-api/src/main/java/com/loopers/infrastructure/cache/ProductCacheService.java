package com.loopers.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductDetailInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 상품 캐시 서비스
 * - Redis를 사용하여 상품 상세 및 목록 조회 성능을 개선한다.
 * - TTL: 상품 상세 5분, 상품 목록 1분
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시 키 접두사
    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_LIST_KEY_PREFIX = "product:list:";

    // TTL 설정
    private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(5);
    private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(1);

    /**
     * 상품 상세 캐시 조회
     */
    public Optional<ProductDetailInfo> getProductDetail(Long productId, String userIdOrNull) {
        String key = buildProductDetailKey(productId, userIdOrNull);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("[캐시 HIT] 상품 상세 조회: productId={}, userId={}", productId, userIdOrNull);
                return Optional.of(objectMapper.readValue(cached, ProductDetailInfo.class));
            }
            log.debug("[캐시 MISS] 상품 상세 조회: productId={}, userId={}", productId, userIdOrNull);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 역직렬화 실패: productId={}, error={}", productId, e.getMessage());
            // 캐시 에러 시 빈 값 반환하여 DB 조회로 폴백
            return Optional.empty();
        } catch (Exception e) {
            log.warn("상품 상세 캐시 조회 중 예외 발생: productId={}, error={}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 상품 상세 캐시 저장
     */
    public void cacheProductDetail(Long productId, String userIdOrNull, ProductDetailInfo productDetail) {
        String key = buildProductDetailKey(productId, userIdOrNull);
        try {
            String value = objectMapper.writeValueAsString(productDetail);
            redisTemplate.opsForValue().set(key, value, PRODUCT_DETAIL_TTL);
            log.debug("[캐시 저장] 상품 상세: productId={}, userId={}, ttl={}초", productId, userIdOrNull, PRODUCT_DETAIL_TTL.toSeconds());
        } catch (JsonProcessingException e) {
            log.warn("상품 상세 캐시 직렬화 실패: productId={}, error={}", productId, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 상세 캐시 저장 중 예외 발생: productId={}, error={}", productId, e.getMessage());
        }
    }

    /**
     * 상품 목록 캐시 조회
     */
    public Optional<List<ProductDetailInfo>> getProductList(String sortType, Long brandIdOrNull) {
        String key = buildProductListKey(sortType, brandIdOrNull);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("[캐시 HIT] 상품 목록 조회: sortType={}, brandId={}", sortType, brandIdOrNull);
                List<ProductDetailInfo> list = objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ProductDetailInfo.class));
                return Optional.of(list);
            }
            log.debug("[캐시 MISS] 상품 목록 조회: sortType={}, brandId={}", sortType, brandIdOrNull);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("상품 목록 캐시 역직렬화 실패: sortType={}, error={}", sortType, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("상품 목록 캐시 조회 중 예외 발생: sortType={}, error={}", sortType, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 상품 목록 캐시 저장
     */
    public void cacheProductList(String sortType, Long brandIdOrNull, List<ProductDetailInfo> productList) {
        String key = buildProductListKey(sortType, brandIdOrNull);
        try {
            String value = objectMapper.writeValueAsString(productList);
            redisTemplate.opsForValue().set(key, value, PRODUCT_LIST_TTL);
            log.debug("[캐시 저장] 상품 목록: sortType={}, brandId={}, ttl={}초", sortType, brandIdOrNull, PRODUCT_LIST_TTL.toSeconds());
        } catch (JsonProcessingException e) {
            log.warn("상품 목록 캐시 직렬화 실패: sortType={}, error={}", sortType, e.getMessage());
        } catch (Exception e) {
            log.warn("상품 목록 캐시 저장 중 예외 발생: sortType={}, error={}", sortType, e.getMessage());
        }
    }

    /**
     * 특정 상품의 캐시 무효화
     * - 상품 정보가 변경되었을 때 호출한다 (예: 좋아요 등록/취소)
     */
    public void evictProductDetail(Long productId) {
        try {
            // 모든 사용자 조합의 캐시 키를 삭제하기 위해 패턴 매칭
            String pattern = PRODUCT_DETAIL_KEY_PREFIX + productId + ":*";
            redisTemplate.keys(pattern).forEach(key -> {
                redisTemplate.delete(key);
                log.debug("[캐시 무효화] 상품 상세: key={}", key);
            });
        } catch (Exception e) {
            log.warn("상품 상세 캐시 무효화 중 예외 발생: productId={}, error={}", productId, e.getMessage());
        }
    }

    /**
     * 상품 목록 캐시 전체 무효화
     * - 상품 정보가 변경되었을 때 호출한다
     */
    public void evictProductList() {
        try {
            String pattern = PRODUCT_LIST_KEY_PREFIX + "*";
            redisTemplate.keys(pattern).forEach(key -> {
                redisTemplate.delete(key);
                log.debug("[캐시 무효화] 상품 목록: key={}", key);
            });
        } catch (Exception e) {
            log.warn("상품 목록 캐시 무효화 중 예외 발생: error={}", e.getMessage());
        }
    }

    // 캐시 키 생성
    private String buildProductDetailKey(Long productId, String userIdOrNull) {
        // userId가 null이면 "anonymous"로 통일
        String userId = (userIdOrNull != null) ? userIdOrNull : "anonymous";
        return PRODUCT_DETAIL_KEY_PREFIX + productId + ":" + userId;
    }

    private String buildProductListKey(String sortType, Long brandIdOrNull) {
        String brandPart = (brandIdOrNull != null) ? String.valueOf(brandIdOrNull) : "all";
        return PRODUCT_LIST_KEY_PREFIX + sortType + ":" + brandPart;
    }
}
