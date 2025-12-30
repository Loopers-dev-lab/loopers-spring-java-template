package com.loopers.domain.stock.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventPublisherImpl implements StockEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishStockProcessed(StockEvents.Processed event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishStockProcessingFailed(StockEvents.ProcessingFailed event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishStockCompensated(StockEvents.Compensated event) {
        eventPublisher.publish(event);
    }
}
