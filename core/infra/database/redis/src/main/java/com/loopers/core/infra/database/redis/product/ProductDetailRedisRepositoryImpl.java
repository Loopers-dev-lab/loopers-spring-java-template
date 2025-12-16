package com.loopers.core.infra.database.redis.product;

import com.loopers.JacksonUtil;
import com.loopers.core.infra.database.redis.product.entity.ProductDetailRedisEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductDetailRedisRepositoryImpl implements ProductDetailRedisRepository {

    private static final String CACHE_KEY_PREFIX = "product:detail:";
    private static final long CACHE_TTL_SECONDS = 300; // 5분

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Optional<ProductDetailRedisEntity> findById(String productId) {
        String cacheKey = generateCacheKey(productId);
        String json = redisTemplate.opsForValue().get(cacheKey);

        if (Objects.isNull(json)) {
            return Optional.empty();
        }

        return Optional.of(JacksonUtil.convertToObject(json, ProductDetailRedisEntity.class));
    }

    @Override
    public void save(ProductDetailRedisEntity entity) {
        String cacheKey = generateCacheKey(entity.getProductId());
        String json = JacksonUtil.convertToString(entity);
        redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void deleteById(String productId) {
        String cacheKey = generateCacheKey(productId);
        redisTemplate.delete(cacheKey);
    }

    private String generateCacheKey(String productId) {
        return CACHE_KEY_PREFIX + productId;
    }
}
