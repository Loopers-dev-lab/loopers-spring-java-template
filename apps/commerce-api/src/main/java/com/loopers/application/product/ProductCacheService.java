package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_VERSION = "v1";
    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:" + CACHE_VERSION + ":detail:";
    private static final String PRODUCT_LIST_KEY_PREFIX = "product:" + CACHE_VERSION + ":list:";
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration LIST_TTL = Duration.ofMinutes(5);

    public ProductDetailInfo getProductDetailWithCache(Long productId) {
        String key = PRODUCT_DETAIL_KEY_PREFIX + productId;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof ProductDetailInfo info) {
            return info;
        }

        Product product = productService.getProduct(productId);
        ProductDetailInfo info = ProductDetailInfo.of(product, product.getLikeCount());

        redisTemplate.opsForValue().set(key, info, DETAIL_TTL);

        return info;
    }

    public ProductListInfo getProductListWithCache(String cacheKey, ProductSearchCondition condition) {
        String key = PRODUCT_LIST_KEY_PREFIX + cacheKey;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof ProductListInfo info) {
            return info;
        }

        Page<Product> productPage = productService.getProducts(condition);
        Map<Long, Long> likeCountMap = productPage.getContent().stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Product::getLikeCount
                ));
        ProductListInfo info = ProductListInfo.of(productPage, likeCountMap);

        redisTemplate.opsForValue().set(key, info, LIST_TTL);

        return info;
    }

    public void evictProductDetailCache(Long productId) {
        String key = PRODUCT_DETAIL_KEY_PREFIX + productId;
        redisTemplate.delete(key);
    }

    public void evictProductListCachesByLikesSort() {
        String pattern = PRODUCT_LIST_KEY_PREFIX + "*:sort:likes_desc:*";
        Set<String> keys = scanRedisKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void evictAllProductListCaches() {
        String pattern = PRODUCT_LIST_KEY_PREFIX + "*";
        Set<String> keys = scanRedisKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void evictAllProductCaches() {
        Set<String> detailKeys = scanRedisKeys(PRODUCT_DETAIL_KEY_PREFIX + "*");
        Set<String> listKeys = scanRedisKeys(PRODUCT_LIST_KEY_PREFIX + "*");

        long deletedCount = 0;
        if (!detailKeys.isEmpty()) {
            deletedCount += redisTemplate.delete(detailKeys);
        }
        if (!listKeys.isEmpty()) {
            deletedCount += redisTemplate.delete(listKeys);
        }
    }

    private Set<String> scanRedisKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        try (var cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        }
        return keys;
    }
}
