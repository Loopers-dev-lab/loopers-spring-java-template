package com.loopers.application.event;

import com.loopers.domain.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxEventHandler {
    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductViewedEvent(ProductViewedEvent event) {
        log.debug("Handling ProductViewedEvent for outbox: userId={}, productId={}", 
            event.userId(), event.productId());
        
        outboxService.saveEvent(
            "Product",
            String.valueOf(event.productId()),
            "ProductViewed",
            event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLikeEvent(LikeEvent event) {
        log.debug("Handling LikeEvent for outbox: userId={}, productId={}, action={}", 
            event.userId(), event.productId(), event.action());
        
        String eventType = event.action() == LikeEvent.LikeAction.LIKE ? "ProductLiked" : "ProductUnliked";
        
        outboxService.saveEvent(
            "Product",
            event.productId().toString(),
            eventType,
            event
        );
    }


    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.debug("Handling OrderPaidEvent for outbox: orderId={}, userId={}", 
            event.orderId(), event.userId());
        
        outboxService.saveEvent(
            "Order",
            event.orderId().toString(),
            "OrderPaid",
            event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        log.debug("Handling OrderCancelledEvent for outbox: orderId={}, userId={}, reason={}", 
            event.orderId(), event.userId(), event.reason());
        
        outboxService.saveEvent(
            "Order",
            event.orderId().toString(),
            "OrderCancelled",
            event
        );
    }
}