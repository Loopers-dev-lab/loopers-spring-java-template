package com.loopers.infrastructure.stock;

import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class StockRepositoryImpl implements StockRepository {
  private final StockJpaRepository jpaRepository;

  @Override
  public Optional<Stock> findByProductId(Long productId) {
    return jpaRepository.findByRefProductId(productId);
  }

  @Override
  public Optional<Stock> findByProductIdForUpdate(Long productId) {
    return jpaRepository.findByProductIdForUpdate(productId);
  }

  @Override
  public Stock save(Stock stock) {
    return jpaRepository.save(stock);
  }
}
