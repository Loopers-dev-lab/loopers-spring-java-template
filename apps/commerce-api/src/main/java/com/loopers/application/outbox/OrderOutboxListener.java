package com.loopers.application.outbox;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.event.OrderCreatedEvent;
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
public class OrderOutboxListener {

    private final OutboxEventService outboxEventService;
    private final OrderRepository orderRepository;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        // 1) 주문 생성 이벤트 자체도 Outbox로 발행
        outboxEventService.saveEvent(
                "ORDER",
                event.orderId(),
                "OrderCreatedEvent",
                event.orderId(),
                event
        );

        // 2) 아이템 단위 메트릭 이벤트 발행
        orderRepository.findById(Long.valueOf(event.orderId()))
                .ifPresent(order -> publishItemMetrics(order));
    }

    private void publishItemMetrics(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            outboxEventService.saveEvent(
                    "ORDER",
                    order.getId().toString(),
                    "ORDER_ITEM_METRIC",
                    String.valueOf(item.getProductId()),
                    Map.of(
                            "productId", item.getProductId(),
                            "quantity", item.getQuantity(),
                            "totalAmount", item.calculateTotalPrice()
                    )
            );
        }
        log.info("Outbox 기록 - ORDER_ITEM_METRIC count={}, orderId={}",
                order.getOrderItems().size(), order.getId());
    }
}



