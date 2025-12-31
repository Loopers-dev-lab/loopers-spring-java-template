package com.loopers.application.product.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.DomainEventEnvelop;
import com.loopers.domain.common.event.DomainEventRepository;
import com.loopers.messaging.event.ProductViewedEvent;
import com.loopers.support.json.JsonConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewedEventHandler {

    private final DomainEventRepository eventRepository;
    private final JsonConverter jsonConverter;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductViewedEvent(ProductViewedEvent event) {
        log.info("ProductViewedEvent received: productId={}, occurredAt={}", event.productId(), event.occurredAt());

        DomainEventEnvelop<ProductViewedEvent> envelop =
                DomainEventEnvelop.of(
                        "PRODUCT_VIEWED",
                        "PRODUCT",
                        event.productId(),
                        event
                );

        eventRepository.save(
                DomainEvent.pending(
                        "catalog-events",
                        envelop,
                        jsonConverter.serialize(envelop)
                )
        );
    }
}
