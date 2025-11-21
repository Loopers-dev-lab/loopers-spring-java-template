package com.loopers.domain.stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository {

    List<Stock> findAllByProductIdIn(List<Long> productIds);

    Optional<Stock> findByProductId(Long productId);

    void save(Stock stock);
}
