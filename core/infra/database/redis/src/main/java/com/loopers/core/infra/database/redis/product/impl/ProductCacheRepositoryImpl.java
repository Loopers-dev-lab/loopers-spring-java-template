package com.loopers.core.infra.database.redis.product.impl;

import com.loopers.core.domain.product.ProductDetail;
import com.loopers.core.domain.product.repository.ProductCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.infra.database.redis.product.ProductDetailRedisRepository;
import com.loopers.core.infra.database.redis.product.entity.ProductDetailRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductCacheRepositoryImpl implements ProductCacheRepository {

    private final ProductDetailRedisRepository repository;

    @Override
    public Optional<ProductDetail> findDetailBy(ProductId productId) {
        return repository.findById(productId.value())
                .map(ProductDetailRedisEntity::to);
    }

    @Override
    public void save(ProductDetail productDetail) {
        repository.save(ProductDetailRedisEntity.from(productDetail));
    }
}
