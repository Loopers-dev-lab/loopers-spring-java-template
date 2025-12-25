package com.loopers.core.infra.database.redis.product;

import com.loopers.core.infra.database.redis.product.entity.ProductDetailRedisEntity;

import java.util.Optional;

public interface ProductDetailRedisRepository {

    Optional<ProductDetailRedisEntity> findById(String productId);

    void save(ProductDetailRedisEntity entity);

    void deleteById(String productId);
}
