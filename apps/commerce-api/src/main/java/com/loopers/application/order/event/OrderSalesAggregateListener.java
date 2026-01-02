package com.loopers.application.order.event;

import com.loopers.domain.event.OutboxService;
import com.loopers.event.ProductStockEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderSalesAggregateListener {

  private final OutboxService outboxService;
  private final ApplicationEventPublisher eventPublisher;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCreated(OrderCreatedEvent event) {

    event.items().forEach(item -> {

      ProductStockEvent kafkaEvent = ProductStockEvent.of(
          item.productId(),
          item.productName(),
          item.quantity(),
          item.remainStock(),
          item.price()
      );

      outboxService.saveEvent("STOCKS_METRICS", String.valueOf(item.productId()), kafkaEvent);
      eventPublisher.publishEvent(kafkaEvent);
    });
  }
}
