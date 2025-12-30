package com.loopers.domain.stock.event;

public interface StockEventPublisher {
    void publishStockProcessed(StockEvents.Processed event);
    void publishStockProcessingFailed(StockEvents.ProcessingFailed event);
    void publishStockCompensated(StockEvents.Compensated event);
}
