package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductRankings;

import java.time.LocalDate;

public interface ProductRankingCacheRepository {

    void increaseDaily(ProductId productId, LocalDate date, Double score);

    ProductRankings getRankings(LocalDate date, int pageNo, int pageSize);
}
