package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.product.event.StockDepletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OutboxEventHandler {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductLikedEvent(ProductLikedEvent event) {
        Outbox outbox = Outbox.create(
                "PRODUCT",
                event.getProductId().toString(),
                "ProductLikedEvent",
                "product-liked",
                toJson(event)
        );
        outboxRepository.save(outbox);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        Outbox outbox = Outbox.create(
                "ORDER",
                event.getOrderId().toString(),
                "OrderCompletedEvent",
                "order-events",
                toJson(event)
        );
        outboxRepository.save(outbox);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleStockDepletedEvent(StockDepletedEvent event) {
        Outbox outbox = Outbox.create(
                "PRODUCT",
                event.getProductId().toString(),
                "StockDepletedEvent",
                "stock-depleted",
                toJson(event)
        );
        outboxRepository.save(outbox);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
