package com.loopers.infrastructure.stock;

import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class StockRepositoryImpl implements StockRepository {

    private final StockJpaRepository stockJpaRepository;
    private final StockQueryRepository stockQueryRepository;

    @Override
    public Optional<Stock> findByProductId(Long productId) {
        return stockJpaRepository.findByProductId(productId);
    }

    @Override
    public Optional<Stock> save(Stock stock) {
        Stock savedStock = stockJpaRepository.save(stock);
        return Optional.of(savedStock);
    }

    @Override
    public long decreaseQuantity(Long productId, Long decreaseQuantity) {
        return stockQueryRepository.decreaseQuantity(productId, decreaseQuantity);
    }

    @Override
    public long increaseQuantity(Long productId, Long increaseQuantity) {
        return stockQueryRepository.increaseQuantity(productId, increaseQuantity);
    }
}

