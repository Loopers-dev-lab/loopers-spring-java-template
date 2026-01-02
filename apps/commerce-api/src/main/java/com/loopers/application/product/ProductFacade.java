package com.loopers.application.product;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.product.ProductSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final RankingFacade rankingFacade;
    private final RedisTemplate<String, Object> productCacheTemplate;
    private final ProductEventPublisher productEventPublisher;

    // 캐시 설정
    private static final String CACHE_PREFIX = "product:detail:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5); // 5분 TTL

    /**
     * 상품 상세 조회 (Cache-Aside 패턴)
     *
     * 1. Redis 캐시 조회 시도
     * 2. 캐시 히트: 캐시 데이터 반환
     * 3. 캐시 미스: DB 조회 후 캐시 저장
     * 4. Redis 장애 시: DB 조회 (Fallback)
     */
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId) {
        String cacheKey = CACHE_PREFIX + productId;

        try {
            // 1. 캐시 조회 시도
            Object cached = productCacheTemplate.opsForValue().get(cacheKey);

            if (cached instanceof ProductDetailInfo cachedProduct) {
                log.debug("Cache HIT for productId: {}", productId);

                // 캐시 히트인 경우에도 이벤트 발행 (별도 트랜잭션)
                productEventPublisher.publishProductViewedEvent(productId);

                return cachedProduct;
            }
        } catch (Exception e) {
            log.warn("Redis read error for productId: {}. Proceeding to DB.", productId, e);
        }

        log.debug("Cache MISS for productId: {}", productId);

        // 2. 캐시 미스: DB 조회 (트랜잭션 컨텍스트 내에서 실행)
        Product product = productService.getProductDetail(productId);
        // 랭킹 정보를 조회( 오늘 날짜 조회 )
        RankingInfo.ProductRankings rankings =
                rankingFacade.getAllRankingsForProduct(productId, null);
        ProductDetailInfo productDetail =
                ProductDetailInfo.of(
                        product,
                        rankings
                );

        // 3. 캐시 저장 (TTL 적용)
        try {
            productCacheTemplate.opsForValue().set(cacheKey, productDetail, CACHE_TTL);
            log.debug("Cached productId: {} with TTL: {}", productId, CACHE_TTL);
        }  catch (Exception e) {
            log.warn("Redis write error for productId: {}", productId, e);
        }

        // 조회 이벤트 발행 (별도 트랜잭션)
        productEventPublisher.publishProductViewedEvent(productId);

        return productDetail;
    }

    /**
     * 캐시 무효화 (상품 수정/삭제 시 사용)
     */
    public void evictProductCache(Long productId) {
        String cacheKey = CACHE_PREFIX + productId;
        try {
            Boolean deleted = productCacheTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Evicted cache for productId: {}", productId);
            }
        } catch (Exception e) {
            log.error("Failed to evict cache for productId: {}", productId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductDetailInfo> getProducts(ProductSearchCondition condition) {

        List<Product> products = productService.getProducts(
                condition.productName(),
                condition.brandId(),
                condition.page(),
                condition.size(),
                condition.sortType()
        );

        return ProductDetailInfo.from(products);
    }

    /**
     * 상품 정보 업데이트 (캐시 무효화 포함)
     *
     * 1. 상품 정보 업데이트
     * 2. Redis 캐시 무효화
     */
    @Transactional
    public ProductDetailInfo updateProduct(Long productId, String productName, java.math.BigDecimal price) {
        // 1. 상품 정보 업데이트
        Product product = productService.updateProduct(productId, productName, price);

        // 2. 캐시 무효화
        evictProductCache(productId);

        return ProductDetailInfo.from(product);
    }

}
