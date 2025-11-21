package com.loopers.infrastructure.stock;

import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class StockRepositoryImpl implements StockRepository {
    private final StockJpaRepository stockJpaRepository;

    @Override
    public List<Stock> findAllByProductIdIn(List<Long> productIds) {
        return stockJpaRepository.findAllByProductIdIn(productIds);
    }

    @Override
    public Optional<Stock> findByProductId(Long productId) {
        return stockJpaRepository.findByProductId(productId);
    }

    @Override
    public void save(Stock stock) {
        stockJpaRepository.save(stock);
    }
}
