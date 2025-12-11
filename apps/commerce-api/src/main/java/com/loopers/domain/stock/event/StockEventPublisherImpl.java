package com.loopers.domain.stock.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EventPublisher 구현체만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
public class StockEventPublisherImpl implements StockEventPublisher {

    private final EventPublisher eventPublisher;
    
    private static final String TOPIC_STOCK_PROCESSED = "stock.deducted.v1";
    private static final String TOPIC_STOCK_FAILED = "stock.deduction-failed.v1";
    private static final String TOPIC_STOCK_COMPENSATED = "stock.compensated.v1";

    @Override
    public void publishStockProcessed(StockEvents.Processed event) {
        eventPublisher.publish(TOPIC_STOCK_PROCESSED, String.valueOf(event.orderId()), event);
    }

    @Override
    public void publishStockProcessingFailed(StockEvents.ProcessingFailed event) {
        eventPublisher.publish(TOPIC_STOCK_FAILED, String.valueOf(event.orderId()), event);
    }

    @Override
    public void publishStockCompensated(StockEvents.Compensated event) {
        eventPublisher.publish(TOPIC_STOCK_COMPENSATED, String.valueOf(event.orderId()), event);
    }
}
