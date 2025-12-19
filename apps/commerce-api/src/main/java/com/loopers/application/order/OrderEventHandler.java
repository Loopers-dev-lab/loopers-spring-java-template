package com.loopers.application.order;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.event.EventTypes;
import com.loopers.domain.event.EventVersions;
import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxRepository;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentTimeoutEvent;
import com.loopers.infrastructure.event.DomainEventEnvelopeFactory;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventHandler {
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final OrderFacade orderFacade;
    private final OrderService orderService;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final OutboxRepository outboxRepository;

    private void executeSafely(String action, Long orderId, Long userId, Runnable task) {
        if (orderId == null || userId == null) {
            log.warn("이벤트 무시 - 필수 값 누락 action={}, orderNumber={}, userId={}", action, orderId, userId);
            return;
        }
        try {
            log.debug("이벤트 처리 시작 action={}, orderNumber={}, userId={}", action, orderId, userId);
            task.run();
            log.info("이벤트 처리 성공 action={}, orderNumber={}, userId={}", action, orderId, userId);
        } catch (Exception e) {
            log.error("이벤트 처리 실패 action={}, orderNumber={}, userId={}", action, orderId, userId, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Long orderId = event.orderNumber();
        Long userId = event.userId();
        
        // 1. 주문 확정 처리
        executeSafely("PAYMENT_COMPLETED", orderId, userId,
                () -> orderFacade.confirmOrderByPayment(orderId, userId));
        
        // 2. Kafka 이벤트용 Outbox 저장
        try {
            savePaymentSuccessToOutbox(event);
        } catch (Exception e) {
            log.error("결제 완료 이벤트 Outbox 저장 실패 - orderNumber={}, userId={}",
                    orderId, userId, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Long orderId = event.orderId();
        Long userId = event.userId();
        executeSafely("PAYMENT_FAILED", orderId, userId,
                () -> orderFacade.cancelOrderByPaymentFailure(orderId, userId));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentTimeout(PaymentTimeoutEvent event) {
        Long orderId = event.orderId();
        Long userId = event.userId();
        executeSafely("PAYMENT_TIMEOUT", orderId, userId,
                () -> orderFacade.cancelOrderByPaymentFailure(orderId, userId));
    }

    /**
     * 결제 완료 이벤트를 Outbox에 저장
     */
    private void savePaymentSuccessToOutbox(PaymentCompletedEvent event) {
        final Long orderNumber = event.orderNumber();
        final Long userId = event.userId();

        if (orderNumber == null || userId == null) {
            log.warn("Outbox 저장 스킵 - 필수값 누락 orderNumber={}, userId={}", orderNumber, userId);
            return;
        }

        final OrderEntity order = orderService.getOrderByOrderNumberAndUserId(orderNumber, userId);
        final List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order);

        final List<PaymentSuccessPayloadV1.Item> items = orderItems.stream()
                .map(oi -> new PaymentSuccessPayloadV1.Item(oi.getProductId(), oi.getQuantity()))
                .toList();

        final PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(order.getId(), items);
        final var envelope = envelopeFactory.create(EventTypes.PAYMENT_SUCCESS, EventVersions.V1, payload);

        // Outbox에 저장 (READY 상태)
        OutboxEventEntity outboxEvent = OutboxEventEntity.ready(
                envelope.eventId(),
                ORDER_EVENTS_TOPIC,
                String.valueOf(order.getId()), // partition key = orderId(PK)
                envelope.eventType(),
                envelope.version(),
                envelope.occurredAtEpochMillis(),
                envelope.payloadJson()
        );

        outboxRepository.save(outboxEvent);

        log.info("결제 완료 이벤트 Outbox 저장 완료 - type={}, orderId={}, itemCount={}",
                EventTypes.PAYMENT_SUCCESS, order.getId(), items.size());
    }
}
