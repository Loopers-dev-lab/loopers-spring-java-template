package com.loopers.core.service.order;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDataPlatformClient;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.service.order.event.OrderCompletedEvent;
import com.loopers.core.service.order.event.OrderDataPlatformSendingFailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataPlatformHandler {

    private final OrderRepository orderRepository;
    private final OrderDataPlatformClient dataPlatformClient;
    private final EventOutboxRepository eventOutboxRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        try {
            Order order = orderRepository.getById(event.orderId());
            dataPlatformClient.send(order);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            eventOutboxRepository.save(
                    EventOutbox.create(
                            AggregateType.ORDER,
                            event.orderId().toAggregateId(),
                            EventType.PAYMENT_DATA_PLATFORM_SENDING_FAILED,
                            new EventPayload(
                                    JacksonUtil.convertToString(
                                            new OrderDataPlatformSendingFailEvent(event.orderId(), e.getMessage())
                                    )
                            )
                    )
            );
        }
    }
}
