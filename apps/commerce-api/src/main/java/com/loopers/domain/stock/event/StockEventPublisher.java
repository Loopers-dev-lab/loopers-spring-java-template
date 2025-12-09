package com.loopers.domain.stock.event;

public interface StockEventPublisher {
    void publishStockProcessed(StockProcessedEvent event);
    void publishStockProcessingFailed(StockProcessingFailedEvent event);
    void publishStockCompensated(StockCompensatedEvent event);
}
