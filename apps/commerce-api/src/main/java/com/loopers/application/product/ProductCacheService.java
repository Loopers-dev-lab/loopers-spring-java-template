package com.loopers.application.product;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class ProductCacheService {

    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.version.product}")
    private String cacheVersion;

    private String productDetailKeyPrefix() {
        return "product:" + cacheVersion + ":detail:";
    }

    private String productListKeyPrefix() {
        return "product:" + cacheVersion + ":list:";
    }

    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration LIST_TTL = Duration.ofMinutes(5);

    public ProductCacheService(
            ProductService productService,
            @Qualifier(RedisConfig.REDIS_TEMPLATE_CACHE) RedisTemplate<String, Object> redisTemplate
    ) {
        this.productService = productService;
        this.redisTemplate = redisTemplate;
    }

    public ProductDetailInfo getProductDetailWithCache(Long productId) {
        String key = productDetailKeyPrefix() + productId;

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
        String key = productListKeyPrefix() + cacheKey;

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
        String key = productDetailKeyPrefix() + productId;
        redisTemplate.delete(key);
    }

    public void evictProductListCachesByLikesSort() {
        String pattern = productListKeyPrefix() + "*:sort:likes_desc:*";
        Set<String> keys = scanRedisKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void evictAllProductListCaches() {
        String pattern = productListKeyPrefix() + "*";
        Set<String> keys = scanRedisKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void evictAllProductCaches() {
        Set<String> detailKeys = scanRedisKeys(productDetailKeyPrefix() + "*");
        Set<String> listKeys = scanRedisKeys(productListKeyPrefix() + "*");

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

        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return keys;
            }
            var connection = connectionFactory.getConnection();
            try (var cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            }
        } catch (Exception e) {
            return keys;
        }
        return keys;
    }
}
