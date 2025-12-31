package com.loopers.application.outbox;

import com.loopers.domain.product.event.ProductDetailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewedOutboxListener {

    private final OutboxEventService outboxEventService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(ProductDetailEvent event) {
        Long productId = event.productId();
        outboxEventService.saveEvent(
                "PRODUCT",
                String.valueOf(productId),
                "PRODUCT_VIEWED",
                String.valueOf(productId),
                Map.of("productId", productId)
        );
        log.info("Outbox 기록 - PRODUCT_VIEWED productId={}", productId);
    }
}



