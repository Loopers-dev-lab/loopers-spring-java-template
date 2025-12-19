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
import com.loopers.infrastructure.event.DomainEventPublisher;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 관련 Kafka 이벤트 발행 핸들러
 * - 결제 완료 이벤트를 Kafka로 발행
 * - 다른 도메인 이벤트는 각각의 전용 핸들러에서 처리
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaEventHandler {
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final OrderService orderService;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final OutboxRepository outboxRepository;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 결제 완료 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(final PaymentCompletedEvent event) {
        final Long orderNumber = event.orderNumber();
        final Long userId = event.userId();

        if (orderNumber == null || userId == null) {
            log.warn("Outbox 적재 스킵 - 필수값 누락 orderNumber={}, userId={}", orderNumber, userId);
            return;
        }


        final OrderEntity order = orderService.getOrderByOrderNumberAndUserId(orderNumber, userId);
        final List<OrderItemEntity> orderItems = orderService.getOrderItemsByOrderId(order);

        final List<PaymentSuccessPayloadV1.Item> items = orderItems.stream()
                .map(oi -> new PaymentSuccessPayloadV1.Item(oi.getProductId(), oi.getQuantity()))
                .toList();

        final PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(order.getId(), items);

        final var envelope = envelopeFactory.create(EventTypes.PAYMENT_SUCCESS, EventVersions.V1, payload);
        final String partitionKey = String.valueOf(order.getId());

        try {
            // 1. 즉시 Kafka 발송 시도
            domainEventPublisher.publish(ORDER_EVENTS_TOPIC, partitionKey, envelope);

            OutboxEventEntity ready = OutboxEventEntity.ready(
                    envelope.eventId(),
                    ORDER_EVENTS_TOPIC,
                    String.valueOf(order.getId()), // partition key = orderId(PK)
                    envelope.eventType(),
                    envelope.version(),
                    envelope.occurredAtEpochMillis(),
                    envelope.payloadJson()
            );
            ready.markSent(); // SENT 상태로 변경

            outboxRepository.save(ready);

            log.info("Outbox 적재 완료 - type={}, orderId={}, itemCount={}",
                    EventTypes.PAYMENT_SUCCESS, order.getId(), items.size());
        } catch (Exception e) {
            log.error("Outbox 적재 실패 - type={}, orderNumber={}, userId={}",
                    EventTypes.PAYMENT_SUCCESS, orderNumber, userId, e);
        }
    }


}
