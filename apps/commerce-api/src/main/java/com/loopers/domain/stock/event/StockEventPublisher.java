package com.loopers.domain.stock.event;

public interface StockEventPublisher {
    void publishStockProcess(StockProcessEvent event);
}
