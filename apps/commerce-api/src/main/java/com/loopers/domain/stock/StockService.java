
package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class StockService {

  private final StockRepository stockRepository;

  @Transactional(readOnly = true)
  public Stock getStock(Long productId) {
    return stockRepository.findByProductId(productId)
        .orElse(Stock.create(productId, 0));
  }

  @Transactional
  public Stock save(Stock stock) {
    return stockRepository.save(stock);
  }

  @Transactional
  public Stock deduct(Long productId, long quantity) {
    Stock stock = stockRepository.findByProductIdForUpdate(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 재고가 존재하지 않습니다."));
    stock.deduct(quantity);
    return stockRepository.save(stock);
  }
}
