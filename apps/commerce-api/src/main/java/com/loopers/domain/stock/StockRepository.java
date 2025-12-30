package com.loopers.domain.stock;

import java.util.Optional;

public interface StockRepository {
    Optional<Stock> findByProductId(Long productId);
    Optional<Stock> save(Stock stock);
    long decreaseQuantity(Long productId, Long decreaseQuantity);
    long increaseQuantity(Long productId, Long increaseQuantity);
}

