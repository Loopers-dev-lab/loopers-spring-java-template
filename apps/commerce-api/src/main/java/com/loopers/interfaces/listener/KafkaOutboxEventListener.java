package com.loopers.interfaces.listener;

import com.loopers.application.kafka.KafkaEvent;
import com.loopers.application.like.LikeEvent;
import com.loopers.application.order.OrderEvent;
import com.loopers.application.product.ProductEvent;
import com.loopers.application.userbehavior.UserBehaviorEvent;
import com.loopers.domain.outbox.OutboxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaOutboxEventListener {

    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCreated(OrderEvent.OrderCreated event) {
        List<KafkaEvent.OrderEvent.OrderItemInfo> orderItemInfos = event.orderItems().stream()
                .map(item -> new KafkaEvent.OrderEvent.OrderItemInfo(
                        item.productId(),
                        item.productName(),
                        item.price(),
                        item.quantity()
                ))
                .toList();
        
        KafkaEvent.OrderEvent.OrderCreated kafkaEvent = KafkaEvent.OrderEvent.OrderCreated.from(
                event.orderKey(),
                event.userId(),
                event.orderId(),
                event.originalTotalPrice(),
                event.discountPrice(),
                orderItemInfos
        );
        outboxService.saveOutbox("order-created-events", event.orderKey(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderPaid(OrderEvent.OrderPaid event) {
        List<KafkaEvent.OrderEvent.OrderItemInfo> orderItemInfos = event.orderItems().stream()
                .map(item -> new KafkaEvent.OrderEvent.OrderItemInfo(
                        item.productId(),
                        item.productName(),
                        item.price(),
                        item.quantity()
                ))
                .toList();
        
        KafkaEvent.OrderEvent.OrderPaid kafkaEvent = KafkaEvent.OrderEvent.OrderPaid.from(
                event.orderKey(),
                event.userId(),
                event.orderId(),
                event.totalPrice(),
                orderItemInfos
        );
        outboxService.saveOutbox("order-paid-events", event.orderKey(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLikeRecorded(LikeEvent.LikeRecorded event) {
        KafkaEvent.ProductEvent.ProductLiked kafkaEvent = KafkaEvent.ProductEvent.ProductLiked.from(
                event.productId(),
                event.userId()
        );
        outboxService.saveOutbox("product-liked-events", event.productId().toString(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLikeCancelled(LikeEvent.LikeCancelled event) {
        KafkaEvent.ProductEvent.ProductUnliked kafkaEvent = KafkaEvent.ProductEvent.ProductUnliked.from(
                event.productId(),
                event.userId()
        );
        outboxService.saveOutbox("product-unliked-events", event.productId().toString(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductViewed(UserBehaviorEvent.ProductViewed event) {
        KafkaEvent.ProductEvent.ProductViewed kafkaEvent = KafkaEvent.ProductEvent.ProductViewed.from(
                event.productId(),
                event.userId()
        );
        outboxService.saveOutbox("product-viewed-events", event.productId().toString(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleStockDepleted(ProductEvent.StockDepleted event) {
        KafkaEvent.ProductEvent.ProductStockDepleted kafkaEvent = KafkaEvent.ProductEvent.ProductStockDepleted.from(
                event.productId(),
                event.remainingStock()
        );
        outboxService.saveOutbox("product-stock-depleted-events", event.productId().toString(), kafkaEvent);
    }
}

