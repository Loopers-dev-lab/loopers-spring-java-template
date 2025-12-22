package com.loopers.core.infra.database.redis.product.impl;

import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.infra.database.redis.product.ProductRankingRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ProductRankingCacheRepositoryImpl implements ProductRankingCacheRepository {

    private final ProductRankingRedisRepository repository;

    @Override
    public void increaseDaily(ProductId productId, LocalDateTime date, Double score) {
        repository.increaseDaily(productId.value(), date, score);
    }
}
