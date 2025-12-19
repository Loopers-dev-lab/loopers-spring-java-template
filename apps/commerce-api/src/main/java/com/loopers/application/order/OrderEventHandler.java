package com.loopers.application.order;

import java.util.ArrayList;
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
     * 원자성과 멱등성을 보장하기 위해 상품별로 개별 이벤트를 생성하여 일괄 저장
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

        if (orderItems.isEmpty()) {
            log.warn("Outbox 저장 스킵 - 주문 상품이 없음 orderId={}", order.getId());
            return;
        }

        // 상품별로 개별 이벤트 생성 및 일괄 저장 (원자성 보장)
        try {
            savePaymentSuccessEventsAtomically(order, orderItems);
            log.info("결제 완료 이벤트 Outbox 일괄 저장 완료 - orderId={}, itemCount={}", 
                    order.getId(), orderItems.size());
        } catch (Exception e) {
            log.error("결제 완료 이벤트 Outbox 저장 실패 - orderId={}, itemCount={}", 
                    order.getId(), orderItems.size(), e);
            throw e; // 상위로 전파하여 전체 트랜잭션 롤백
        }
    }

    /**
     * 상품별 결제 완료 이벤트를 원자적으로 Outbox에 저장
     * 모든 상품 이벤트가 성공하거나 모두 실패하도록 보장
     * 새로운 구조: 각 상품별로 주문 컨텍스트 정보를 포함한 개별 이벤트 생성
     */
    private void savePaymentSuccessEventsAtomically(OrderEntity order, List<OrderItemEntity> orderItems) {
        final List<OutboxEventEntity> outboxEvents = new ArrayList<>();
        final long baseTimestamp = System.currentTimeMillis();

        // 각 상품별로 개별 이벤트 생성
        for (int i = 0; i < orderItems.size(); i++) {
            final OrderItemEntity orderItem = orderItems.get(i);
            
            // 상품별 개별 페이로드 생성 (주문 컨텍스트 정보 포함)
            final PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(
                    order.getId(),              // 주문 내부 ID (PK)
                    order.getOrderNumber(),     // 주문 번호 (비즈니스 키)
                    order.getUserId(),          // 주문한 사용자 ID
                    orderItem.getProductId(),   // 상품 ID (개별 이벤트)
                    orderItem.getQuantity(),    // 구매 수량 (개별 이벤트)
                    orderItem.getUnitPrice(),   // 단가
                    orderItem.getTotalPrice()   // 해당 상품의 총 금액
            );

            // 각 상품별로 고유한 이벤트 ID와 타임스탬프 생성 (순서 보장)
            final var envelope = envelopeFactory.create(
                    EventTypes.PAYMENT_SUCCESS, 
                    EventVersions.V1, 
                    payload
            );

            // 복합 파티션 키 사용: "orderId-productId" (동일 주문의 동일 상품은 동일 파티션)
            final String partitionKey = String.format("%d-%d", order.getId(), orderItem.getProductId());

            final OutboxEventEntity outboxEvent = OutboxEventEntity.ready(
                    envelope.eventId(),
                    ORDER_EVENTS_TOPIC,
                    partitionKey, // 주문ID-상품ID 복합 키로 파티션 분산
                    envelope.eventType(),
                    envelope.version(),
                    baseTimestamp + i, // 순서 보장을 위한 타임스탬프 조정
                    envelope.payloadJson()
            );

            outboxEvents.add(outboxEvent);
            
            log.debug("상품별 결제 완료 이벤트 생성 - orderId={}, orderNumber={}, productId={}, quantity={}, unitPrice={}, totalPrice={}, eventId={}", 
                    order.getId(), order.getOrderNumber(), orderItem.getProductId(), 
                    orderItem.getQuantity(), orderItem.getUnitPrice(), orderItem.getTotalPrice(), envelope.eventId());
        }

        // 모든 이벤트를 일괄 저장 (원자성 보장)
        outboxRepository.saveAll(outboxEvents);
        
        log.info("상품별 결제 완료 이벤트 일괄 저장 완료 - orderId={}, orderNumber={}, eventCount={}", 
                order.getId(), order.getOrderNumber(), outboxEvents.size());
    }
}
