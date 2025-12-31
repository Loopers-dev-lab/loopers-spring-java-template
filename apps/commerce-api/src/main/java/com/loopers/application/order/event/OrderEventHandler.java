package com.loopers.application.order.event;

import com.loopers.application.order.OrderExternalSystemSender;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.DomainEventEnvelop;
import com.loopers.domain.common.event.DomainEventRepository;
import com.loopers.messaging.event.OrderPaidEvent;
import com.loopers.support.json.JsonConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderFacade orderFacade;
    private final OrderExternalSystemSender orderExternalSystemSender;

    private final DomainEventRepository eventRepository;
    private final JsonConverter jsonConverter;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OrderPaidEvent event) {
        log.info("🔥 OrderPaidEvent handler 진입");
        orderFacade.handleOrderSucceed(event.orderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PaymentFailedEvent event) {
        orderFacade.handleOrderFailure(event.userId(), event.orderId());
    }

    /**
     * 결제 성공 시, 외부 시스템으로 주문 / 결제 정보를 전송합니다.
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void handleOrderCreatedExternalSend(OrderPaidEvent event) {
        try {
            orderExternalSystemSender.send(event.orderId());
        } catch (Exception e) {
            log.error("외부 시스템으로의 주문 전송 실패, 주문 ID: {}", event.orderId(), e);
        }
    }


    /**
     * 결제 성공 시, outbox 테이블에 이벤트를 저장합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOutboxEvent(OrderPaidEvent event) {

        DomainEventEnvelop<OrderPaidEvent> envelop =
                DomainEventEnvelop.of(
                        "ORDER_PAID",
                        "ORDER",
                        event.orderId(),
                        event
                );

        eventRepository.save(
                DomainEvent.pending(
                        "order-events",
                        envelop,
                        jsonConverter.serialize(envelop)
                )
        );
    }
}
