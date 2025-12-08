package com.loopers.domain.stock.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventPublisherImpl implements StockEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishStockProcess(StockProcessEvent event) {
        eventPublisher.publishEvent(event);
    }
}
