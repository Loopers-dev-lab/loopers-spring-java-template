package com.loopers.domain.stock;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    public Map<Long, Stock> getStocksByProductIds(List<Long> productIds) {
        List<Stock> stocks = stockRepository.findAllByProductIdIn(productIds);
        if(stocks.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "상품의 재고가 존재하지 않습니다.");
        }
        return stocks.stream()
                .collect(Collectors.toMap(Stock::getProductId, stock -> stock));
    }

    public Stock getStockByProductId(Long productId) {
        return stockRepository.findByProductId(productId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "재고가 존재하지 않는 상품ID 입니다.")
        );
    }
}
