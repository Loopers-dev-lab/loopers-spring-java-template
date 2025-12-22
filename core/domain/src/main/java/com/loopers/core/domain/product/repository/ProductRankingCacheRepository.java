package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.vo.ProductId;

import java.time.LocalDateTime;

public interface ProductRankingCacheRepository {

    void increaseDaily(ProductId productId, LocalDateTime localDateTime, Double score);
}
