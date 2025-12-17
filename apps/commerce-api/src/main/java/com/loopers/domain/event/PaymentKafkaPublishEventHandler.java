package com.loopers.domain.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.event.outbox.OutboxEventEntity;
import com.loopers.domain.event.outbox.OutboxRepository;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.infrastructure.event.DomainEventEnvelopeFactory;
import com.loopers.infrastructure.event.outbox.OutboxEventJpaRepository;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaPublishEventHandler {
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final OrderService orderService;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final OutboxRepository outboxRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final PaymentCompletedEvent event) {
        // PaymentCompletedEvent의 orderNumber 는 주문 번호
        final Long orderNumber = event.orderNumber();
        final Long userId = event.userId();

        if (orderNumber == null || userId == null) {
            log.warn("Outbox 적재 스킵 - 필수값 누락 orderNumber={}, userId={}", orderNumber, userId);
            return;
        }

        try {
            final OrderEntity order = orderService.getOrderByOrderNumberAndUserId(orderNumber, userId);
            final List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order);

            final List<PaymentSuccessPayloadV1.Item> items = orderItems.stream()
                    .map(oi -> new PaymentSuccessPayloadV1.Item(oi.getProductId(), oi.getQuantity()))
                    .toList();

            final PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(order.getId(), items);

            final var envelope = envelopeFactory.create(EventTypes.PAYMENT_SUCCESS, EventVersions.V1, payload);

            OutboxEventEntity ready = OutboxEventEntity.ready(
                    envelope.eventId(),
                    ORDER_EVENTS_TOPIC,
                    String.valueOf(order.getId()), // partition key = orderId(PK)
                    envelope.eventType(),
                    envelope.version(),
                    envelope.occurredAtEpochMillis(),
                    envelope.payloadJson()
            );

            outboxRepository.save(ready);

            log.info("Outbox 적재 완료 - type={}, orderId={}, itemCount={}",
                    EventTypes.PAYMENT_SUCCESS, order.getId(), items.size());
        } catch (Exception e) {
            log.error("Outbox 적재 실패 - type={}, orderNumber={}, userId={}",
                    EventTypes.PAYMENT_SUCCESS, orderNumber, userId, e);
        }
    }
}
