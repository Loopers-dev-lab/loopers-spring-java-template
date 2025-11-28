package com.loopers.domain.product.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.infrastructure.product.ProductQueryRepository;
import com.loopers.support.config.CacheProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private static final String PRODUCT_LIST_KEY_PREFIX = "products::list::";
    private static final String PRODUCT_INFO_KEY_PREFIX = "product:info:";
    private static final String PRODUCT_STAT_KEY_PREFIX = "product:stat:";
    private static final String LIKE_COUNT_FIELD = "likeCount";

    private final RedisTemplate<String, Object> productCacheRedisTemplate;
    private final RedisTemplate<String, String> productListCacheRedisTemplate;
    private final ProductQueryRepository productQueryRepository;
    private final ProductViewRepository productViewRepository;
    private final CacheProperties cacheProperties;
    private final ObjectMapper objectMapper;

    // 애플리케이션 시작 시 랜덤하게 선택된 Hot ID Set (초기화는 @PostConstruct에서)
    private Set<Long> hotBrandIds;
    private Set<Long> hotProductIds;

    /**
     * 애플리케이션 시작 시 Hot ID Set 초기화
     */
    @PostConstruct
    public void initHotIds() {
        hotBrandIds = generateRandomIds(
                cacheProperties.hotTargets().brandIdRange().min(),
                cacheProperties.hotTargets().brandIdRange().max(),
                cacheProperties.hotTargets().brandIdRange().count()
        );
        hotProductIds = generateRandomIds(
                cacheProperties.hotTargets().productIdRange().min(),
                cacheProperties.hotTargets().productIdRange().max(),
                cacheProperties.hotTargets().productIdRange().count()
        );
        log.info("Hot Brand IDs initialized: {} IDs in range [{}, {}]", 
                hotBrandIds.size(), 
                cacheProperties.hotTargets().brandIdRange().min(),
                cacheProperties.hotTargets().brandIdRange().max());
        log.info("Hot Product IDs initialized: {} IDs in range [{}, {}]", 
                hotProductIds.size(),
                cacheProperties.hotTargets().productIdRange().min(),
                cacheProperties.hotTargets().productIdRange().max());
    }

    /**
     * 범위에서 순차적으로 지정된 개수만큼 ID 선택 (테스트 가능성을 위해 랜덤 제거)
     */
    private Set<Long> generateRandomIds(Long min, Long max, Integer count) {
        if (min == null || max == null || count == null || min > max || count <= 0) {
            return Collections.emptySet();
        }

        long rangeSize = max - min + 1;
        int actualCount = Math.min(count, (int) rangeSize); // 범위보다 많이 요청하면 전체 범위 사용

        Set<Long> ids = new LinkedHashSet<>();
        
        // 순차적으로 ID 생성 (1부터 count까지)
        for (long i = min; i < min + actualCount; i++) {
            ids.add(i);
        }

        return Collections.unmodifiableSet(ids);
    }

    /**
     * 상품 목록 조회 (Two-Tier Caching)
     * L1: ID 리스트 캐싱
     * L2: 상품 상세 정보 캐싱
     */
    public Page<ProductView> getProductViews(ProductCondition condition, Pageable pageable) {
        // 1. L1: ID 리스트 조회 (캐시 키 생성)
        String listKey = buildListKey(condition, pageable);
        boolean isHotPage = isHotPage(condition, pageable);

        List<Long> productIds = getProductIdsFromCache(listKey, condition, pageable, isHotPage);

        if (productIds.isEmpty()) {
            // 캐시 미스: DB에서 조회
            Page<ProductView> dbPage = productQueryRepository.findProductViews(condition, pageable);
            productIds = dbPage.getContent().stream()
                    .map(ProductView::getId)
                    .toList();

            // Cold Page는 락 없이 캐시 저장
            if (!isHotPage) {
                saveProductIdsToCache(listKey, productIds);
            } else {
                // Hot Page는 PER 적용하여 저장
                saveProductIdsToCacheWithPER(listKey, productIds);
            }
        }

        // 2. L2: 상품 상세 정보 조회 (MGET)
        List<ProductView> productViews = getProductViewsByIds(productIds);

        // 3. 전체 개수 조회 (효율적인 count 쿼리 사용)
        long total = productQueryRepository.countProductViews(condition);

        return new PageImpl<>(productViews, pageable, total);
    }

    /**
     * 상품 상세 조회
     * Hot Product에는 PER(Probabilistic Early Recomputation) 적용
     */
    public Optional<ProductView> getProductView(Long productId) {
        // L2 캐시에서 조회
        String infoKey = PRODUCT_INFO_KEY_PREFIX + productId;
        String statKey = PRODUCT_STAT_KEY_PREFIX + productId;

        ProductView cached = getProductViewFromCache(infoKey, statKey);
        if (cached != null) {
            // Hot Product인 경우 PER 적용: 만료 임박 시 비동기로 갱신
            if (isHotProduct(productId)) {
                Long ttl = productCacheRedisTemplate.getExpire(infoKey);
                if (ttl != null && ttl > 0 && shouldRefreshProductEarly(ttl)) {
                    refreshProductViewCacheAsync(productId, infoKey, statKey);
                }
            }
            return Optional.of(cached);
        }

        // 캐시 미스: DB에서 조회 후 캐시 저장
        Optional<ProductView> dbView = productViewRepository.findById(productId);

        if (dbView.isPresent()) {
            ProductView productView = dbView.get();
            saveProductViewToCache(productView, infoKey, statKey);
            return Optional.of(productView);
        }

        return Optional.empty();
    }

    /**
     * PER: 상품 상세 캐시 조기 갱신 여부 결정
     * TTL이 짧아질수록 갱신 확률 증가
     */
    private boolean shouldRefreshProductEarly(Long remainingTtl) {
        int baseTtl = cacheProperties.ttl().infoSeconds();
        double probability = 1.0 - ((double) remainingTtl / baseTtl);
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /**
     * 비동기로 상품 상세 캐시 갱신 (PER)
     */
    @Async
    public void refreshProductViewCacheAsync(Long productId, String infoKey, String statKey) {
        try {
            Optional<ProductView> dbView = productViewRepository.findById(productId);
            if (dbView.isPresent()) {
                saveProductViewToCache(dbView.get(), infoKey, statKey);
                log.debug("PER: Product cache refreshed early for productId={}", productId);
            }
        } catch (Exception e) {
            log.warn("PER: Failed to refresh product cache early: productId={}", productId, e);
        }
    }

    /**
     * L1: ID 리스트 캐시에서 조회
     */
    private List<Long> getProductIdsFromCache(String listKey, ProductCondition condition, Pageable pageable, boolean isHotPage) {
        String cachedIdsJson = productListCacheRedisTemplate.opsForValue().get(listKey);
        
        if (cachedIdsJson != null) {
            // PER 적용: Hot Page인 경우 확률적으로 조기 갱신
            if (isHotPage) {
                Long ttl = productListCacheRedisTemplate.getExpire(listKey);
                if (ttl != null && ttl > 0 && shouldRefreshEarly(ttl)) {
                    // 백그라운드에서 갱신 (비동기)
                    refreshProductIdsCacheAsync(listKey, condition, pageable);
                }
            }

            try {
                return objectMapper.readValue(cachedIdsJson, new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize product IDs from cache: key={}", listKey, e);
            }
        }

        return Collections.emptyList();
    }

    /**
     * PER: 확률적 조기 갱신 여부 결정
     * TTL이 짧아질수록 갱신 확률 증가
     */
    private boolean shouldRefreshEarly(Long remainingTtl) {
        int baseTtl = cacheProperties.ttl().listSeconds();
        double probability = 1.0 - ((double) remainingTtl / baseTtl);
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /**
     * 비동기로 ID 리스트 캐시 갱신 (PER - Probabilistic Early Recomputation)
     * @Async를 사용하여 유저 응답 지연 없이 백그라운드에서 갱신
     */
    @Async
    public void refreshProductIdsCacheAsync(String listKey, ProductCondition condition, Pageable pageable) {
        try {
            Page<ProductView> dbPage = productQueryRepository.findProductViews(condition, pageable);
            List<Long> productIds = dbPage.getContent().stream()
                    .map(ProductView::getId)
                    .toList();
            saveProductIdsToCacheWithPER(listKey, productIds);
            log.debug("PER: Cache refreshed early for key={}", listKey);
        } catch (Exception e) {
            log.warn("PER: Failed to refresh cache early: key={}", listKey, e);
        }
    }

    /**
     * ID 리스트를 캐시에 저장 (Cold Page)
     */
    private void saveProductIdsToCache(String listKey, List<Long> productIds) {
        try {
            String json = objectMapper.writeValueAsString(productIds);
            productListCacheRedisTemplate.opsForValue().set(
                    listKey,
                    json,
                    Duration.ofSeconds(cacheProperties.ttl().listSeconds())
            );
        } catch (Exception e) {
            log.warn("Failed to cache product IDs: key={}", listKey, e);
        }
    }

    /**
     * ID 리스트를 캐시에 저장 (Hot Page, PER 적용)
     */
    private void saveProductIdsToCacheWithPER(String listKey, List<Long> productIds) {
        try {
            String json = objectMapper.writeValueAsString(productIds);
            // PER: TTL에 랜덤 요소 추가하여 동시 만료 방지
            int baseTtl = cacheProperties.ttl().listSeconds();
            int randomOffset = ThreadLocalRandom.current().nextInt(0, 60); // 0~60초 랜덤
            productListCacheRedisTemplate.opsForValue().set(
                    listKey,
                    json,
                    Duration.ofSeconds(baseTtl + randomOffset)
            );
        } catch (Exception e) {
            log.warn("Failed to cache product IDs with PER: key={}", listKey, e);
        }
    }

    /**
     * L2: 상품 상세 정보 조회 (MGET)
     */
    private List<ProductView> getProductViewsByIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        // MGET으로 Info를 한번에 조회
        List<String> infoKeys = productIds.stream()
                .map(id -> PRODUCT_INFO_KEY_PREFIX + id)
                .toList();

        List<Object> infoValues = productCacheRedisTemplate.opsForValue().multiGet(infoKeys);
        
        // Stat은 각 키마다 개별 조회 (Hash 구조 특성상)
        List<Object> statValues = new ArrayList<>();
        for (Long productId : productIds) {
            String statKey = PRODUCT_STAT_KEY_PREFIX + productId;
            Object statValue = productCacheRedisTemplate.opsForHash().get(statKey, LIKE_COUNT_FIELD);
            statValues.add(statValue);
        }

        // 캐시 미스인 항목들을 수집
        List<Long> missIds = new ArrayList<>();
        List<ProductView> result = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Object infoValue = infoValues != null && i < infoValues.size() ? infoValues.get(i) : null;
            Object statValue = statValues != null && i < statValues.size() ? statValues.get(i) : null;

            if (infoValue != null) {
                try {
                    ProductView productView = objectMapper.convertValue(infoValue, ProductView.class);
                    
                    // Stat에서 좋아요 수 가져오기
                    if (statValue != null) {
                        Long likeCount = parseLikeCount(statValue);
                        if (likeCount != null) {
                            productView.updateLikeCount(likeCount);
                        }
                    } else {
                        // Stat이 없으면 DB에서 조회하여 채우기 (Backfill)
                        Long likeCount = getLikeCountFromDb(productId);
                        if (likeCount != null) {
                            productView.updateLikeCount(likeCount);
                            // Stat 캐시에 저장
                            saveStatToCache(productId, likeCount);
                        }
                    }

                    result.add(productView);
                } catch (Exception e) {
                    log.warn("Failed to deserialize ProductView from cache: productId={}", productId, e);
                    missIds.add(productId);
                }
            } else {
                missIds.add(productId);
            }
        }

        // 캐시 미스 항목들을 DB에서 조회하여 채우기 (Backfill)
        if (!missIds.isEmpty()) {
            List<ProductView> dbViews = getProductViewsFromDb(missIds);
            for (ProductView dbView : dbViews) {
                String infoKey = PRODUCT_INFO_KEY_PREFIX + dbView.getId();
                String statKey = PRODUCT_STAT_KEY_PREFIX + dbView.getId();
                saveProductViewToCache(dbView, infoKey, statKey);
                result.add(dbView);
            }
        }

        // 원래 순서 유지
        Map<Long, ProductView> viewMap = result.stream()
                .collect(Collectors.toMap(ProductView::getId, v -> v));
        return productIds.stream()
                .map(viewMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 단일 상품 상세 정보 캐시에서 조회
     */
    private ProductView getProductViewFromCache(String infoKey, String statKey) {
        Object infoValue = productCacheRedisTemplate.opsForValue().get(infoKey);
        if (infoValue == null) {
            return null;
        }

        try {
            ProductView productView = objectMapper.convertValue(infoValue, ProductView.class);
            
            // Stat에서 좋아요 수 가져오기
            Object statValue = productCacheRedisTemplate.opsForHash().get(statKey, LIKE_COUNT_FIELD);
            if (statValue != null) {
                Long likeCount = parseLikeCount(statValue);
                if (likeCount != null) {
                    productView.updateLikeCount(likeCount);
                }
            }

            return productView;
        } catch (Exception e) {
            log.warn("Failed to deserialize ProductView from cache: key={}", infoKey, e);
            return null;
        }
    }

    /**
     * 상품 상세 정보를 캐시에 저장
     */
    private void saveProductViewToCache(ProductView productView, String infoKey, String statKey) {
        try {
            // Info 저장 (긴 TTL)
            productCacheRedisTemplate.opsForValue().set(
                    infoKey,
                    productView,
                    Duration.ofSeconds(cacheProperties.ttl().infoSeconds())
            );

            // Stat 저장 (짧은 TTL, PER 적용) - String.valueOf()로 직렬화
            int baseTtl = cacheProperties.ttl().statSeconds();
            int randomOffset = ThreadLocalRandom.current().nextInt(0, 60);
            productCacheRedisTemplate.opsForHash().put(statKey, LIKE_COUNT_FIELD, String.valueOf(productView.getLikeCount()));
            productCacheRedisTemplate.expire(statKey, Duration.ofSeconds(baseTtl + randomOffset));
        } catch (Exception e) {
            log.warn("Failed to cache ProductView: productId={}", productView.getId(), e);
        }
    }

    /**
     * Stat만 캐시에 저장
     */
    private void saveStatToCache(Long productId, Long likeCount) {
        try {
            String statKey = PRODUCT_STAT_KEY_PREFIX + productId;
            int baseTtl = cacheProperties.ttl().statSeconds();
            int randomOffset = ThreadLocalRandom.current().nextInt(0, 60);
            // String.valueOf()로 직렬화
            productCacheRedisTemplate.opsForHash().put(statKey, LIKE_COUNT_FIELD, String.valueOf(likeCount));
            productCacheRedisTemplate.expire(statKey, Duration.ofSeconds(baseTtl + randomOffset));
        } catch (Exception e) {
            log.warn("Failed to cache stat: productId={}", productId, e);
        }
    }

    /**
     * DB에서 상품 상세 정보 조회
     */
    private List<ProductView> getProductViewsFromDb(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        // ProductViewRepository를 통해 IN 쿼리로 한번에 조회
        return productViewRepository.findByIds(productIds);
    }

    /**
     * DB에서 좋아요 수 조회
     */
    private Long getLikeCountFromDb(Long productId) {
        // ProductViewRepository에서 조회
        return productViewRepository.findById(productId)
                .map(ProductView::getLikeCount)
                .orElse(0L);
    }

    /**
     * 좋아요 수 파싱
     */
    private Long parseLikeCount(Object value) {
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
                return null;
            }
        }
        return null;
    }

    /**
     * Hot Page 여부 확인
     * application.yml의 hot-page-range 설정에 따라 Hot 페이지 범위 결정
     * 메인 페이지(brandId 없음) 또는 Hot Brand의 페이지만 Hot으로 처리
     */
    private boolean isHotPage(ProductCondition condition, Pageable pageable) {
        Integer hotPageRange = cacheProperties.hotTargets().hotPageRange();
        if (hotPageRange == null || hotPageRange <= 0) {
            return false;
        }

        // 페이지 범위 확인 (1~N 페이지, page 0 ~ N-1)
        int pageNumber = pageable.getPageNumber();
        if (pageNumber < 0 || pageNumber >= hotPageRange) {
            return false;
        }

        // 메인 페이지(brandId 없음)는 항상 Hot
        if (condition.brandId() == null) {
            return true;
        }

        // 브랜드 페이지는 Hot Brand인 경우만 Hot
        return isHotBrand(condition.brandId());
    }

    /**
     * Hot Brand 여부 확인
     * application.yml의 brand-id-range 설정에 따라 판단
     */
    public boolean isHotBrand(Long brandId) {
        if (brandId == null || hotBrandIds == null) {
            return false;
        }
        return hotBrandIds.contains(brandId);
    }

    /**
     * Hot Product 여부 확인
     * application.yml의 product-id-range 설정에 따라 판단
     */
    public boolean isHotProduct(Long productId) {
        if (productId == null || hotProductIds == null) {
            return false;
        }
        return hotProductIds.contains(productId);
    }


    /**
     * 목록 캐시 키 생성
     */
    private String buildListKey(ProductCondition condition, Pageable pageable) {
        StringBuilder key = new StringBuilder(PRODUCT_LIST_KEY_PREFIX);
        
        if (condition.brandId() != null) {
            key.append("brand:").append(condition.brandId()).append("::");
        }
        
        key.append("sort:").append(condition.sort() != null ? condition.sort() : "latest").append("::");
        key.append("page:").append(pageable.getPageNumber()).append("::");
        key.append("size:").append(pageable.getPageSize());
        
        return key.toString();
    }
}

