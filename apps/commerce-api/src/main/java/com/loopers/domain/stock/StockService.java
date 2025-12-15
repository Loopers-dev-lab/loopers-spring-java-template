package com.loopers.domain.stock;

import com.loopers.domain.order.OrderItem;
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

    public void decreaseStocks(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Map<Long, Stock> stocksById = getStocksByProductIds(
                items.stream().map(OrderItem::getProductId).sorted().toList()
        );

        for (OrderItem item : items) {
            Stock stock = stocksById.get(item.getProductId());
            if (stock == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "재고가 존재하지 않는 상품ID 입니다. productId: {}" + item.getProductId());
            }
            stock.decrease(item.getQuantity());
        }
    }

    public void increaseStocks(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Map<Long, Stock> stocksById = getStocksByProductIds(
                items.stream().map(OrderItem::getProductId).sorted().toList()
        );

        for (OrderItem item : items) {
            Stock stock = stocksById.get(item.getProductId());
            if (stock == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "재고가 존재하지 않는 상품ID 입니다. productId: {}" + item.getProductId());
            }
            stock.increase(item.getQuantity());
        }
    }
}
