package com.loopers.domain.stock.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventPublisherImpl implements StockEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishStockProcessed(StockProcessedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishStockProcessingFailed(StockProcessingFailedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishStockCompensated(StockCompensatedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
