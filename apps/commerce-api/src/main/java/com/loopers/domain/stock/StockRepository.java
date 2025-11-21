package com.loopers.domain.stock;

import java.util.Optional;

public interface StockRepository {
  Optional<Stock> findByProductId(Long refProductId);

  Optional<Stock> findByProductIdForUpdate(Long refProductId);

  Stock save(Stock stock);

}
