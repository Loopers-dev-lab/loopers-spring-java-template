package com.loopers.core.service.order;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDataPlatformClient;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.service.order.component.OrderDataPlatformSendingFailHandler;
import com.loopers.core.service.order.event.OrderCompletedEvent;
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
    private final OrderDataPlatformSendingFailHandler orderDataPlatformSendingFailHandler;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        try {
            Order order = orderRepository.getById(event.orderId());
            dataPlatformClient.send(order);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            orderDataPlatformSendingFailHandler.handle(event.orderId(), e);
        }
    }
}
