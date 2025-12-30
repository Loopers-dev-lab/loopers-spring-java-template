package com.loopers.infrastructure.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCache;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisProductCache implements ProductCache {

    private static final Duration TTL_LIST = Duration.ofMinutes(10);
    private static final Duration TTL_DETAIL = Duration.ofMinutes(5);
    private static final Logger log = LoggerFactory.getLogger(RedisProductCache.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<Page<Product>> getProductList(Long brandId, Pageable pageable) {

        String key = listKey(brandId, pageable);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) return Optional.empty();

        try {
            Page<Product> page =
                    objectMapper.readValue(json, new TypeReference<PageImpl<Product>>() {
                    });
            return Optional.of(page);
        } catch (Exception e) {
            log.warn("Failed to deserialize product list from cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void putProductList(Long brandId, Pageable pageable, Page<Product> products) {
        String key = listKey(brandId, pageable);

        try {
            String json = objectMapper.writeValueAsString(products);
            redisTemplate.opsForValue().set(key, json, TTL_LIST);
        } catch (Exception e) {
            log.warn("Failed to cache product list for key: {}", key, e);
        }
    }

    @Override
    public Optional<Product> getProductDetail(Long productId) {
        String key = detailKey(productId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) return Optional.empty();

        try {
            Product product = objectMapper.readValue(json, Product.class);
            return Optional.of(product);
        } catch (Exception e) {
            log.warn("Failed to deserialize product detail from cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void putProductDetail(Long productId, Product product) {
        String key = detailKey(productId);
        try {
            String json = objectMapper.writeValueAsString(product);
            redisTemplate.opsForValue().set(key, json, TTL_DETAIL);
        } catch (Exception e) {
            log.warn("Failed to cache product detail for key: {}", key, e);
        }
    }

    private String listKey(Long brandId, Pageable pageable) {
        String sortKey = pageable.getSort().toString();
        return "product:list:"
                + (brandId == null ? "all" : brandId) + ":"
                + pageable.getPageNumber() + ":"
                + pageable.getPageSize() + ":"
                + sortKey;
    }

    private String detailKey(Long productId) {
        return "product:detail:" + productId;
    }
}
