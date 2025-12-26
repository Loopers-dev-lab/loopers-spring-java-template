package com.loopers.infrastructure.order;

import com.loopers.domain.event.outbox.OutboxEventService;
import com.loopers.domain.order.OrderEvent;
import com.loopers.domain.order.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 이벤트를 Kafka로 발행하기 위한 Publisher
 * 
 * 변경 사항:
 * - 기존: ApplicationEventPublisher를 통한 동기식 이벤트 발행
 * - 변경: Outbox 패턴을 통한 비동기 Kafka 이벤트 발행
 * 
 * 처리 플로우:
 * 1. OrderEvent를 받아서 Outbox 테이블에 저장 (같은 트랜잭션)
 * 2. OutboxPublisher가 주기적으로 Kafka로 발행
 * 3. Consumer가 Kafka에서 수신하여 처리 (주문 완료 시 판매량 집계 등)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisherImpl implements OrderEventPublisher {
    private final OutboxEventService outboxEventService;

    @Override
    public void publishOrderCreated(OrderEvent.OrderCreatedEvent event) {
        if (event == null) {
            log.warn("OrderCreatedEvent가 null입니다. 이벤트 발행을 건너뜁니다.");
            return;
        }

        try {
            // 이벤트 페이로드 생성
            Map<String, Object> eventPayload = createOrderCreatedPayload(event);
            
            // Outbox 테이블에 저장 (도메인 트랜잭션 내에서 호출되어야 함)
            outboxEventService.saveEvent(
                    "ORDER",
                    event.getOrderId().toString(),
                    "OrderCreated",
                    eventPayload
            );
            
            log.info("OrderCreatedEvent saved to outbox: orderId={}, orderPublicId={}", 
                    event.getOrderId(), event.getOrderPublicId());
        } catch (Exception e) {
            log.error("Failed to save OrderCreatedEvent to outbox: orderId={}", 
                    event.getOrderId(), e);
            throw e;
        }
    }

    @Override
    public void publishOrderPaid(OrderEvent.OrderPaidEvent event) {
        if (event == null) {
            log.warn("OrderPaidEvent가 null입니다. 이벤트 발행을 건너뜁니다.");
            return;
        }

        try {
            // 이벤트 페이로드 생성
            Map<String, Object> eventPayload = createOrderPaidPayload(event);
            
            // Outbox 테이블에 저장 (도메인 트랜잭션 내에서 호출되어야 함)
            outboxEventService.saveEvent(
                    "ORDER",
                    event.getOrderId().toString(),
                    "OrderPaid",
                    eventPayload
            );
            
            log.info("OrderPaidEvent saved to outbox: orderId={}, orderPublicId={}, transactionKey={}", 
                    event.getOrderId(), event.getOrderPublicId(), event.getTransactionKey());
        } catch (Exception e) {
            log.error("Failed to save OrderPaidEvent to outbox: orderId={}", 
                    event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * OrderCreatedEvent를 Kafka 이벤트 페이로드로 변환
     */
    private Map<String, Object> createOrderCreatedPayload(OrderEvent.OrderCreatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "OrderCreated");
        payload.put("aggregateId", event.getOrderId().toString());
        payload.put("orderId", event.getOrderId());
        payload.put("orderPublicId", event.getOrderPublicId());
        payload.put("userId", event.getUserId());
        payload.put("couponId", event.getCouponId());
        payload.put("finalPrice", event.getFinalPrice() != null ? event.getFinalPrice().amount() : null);
        payload.put("paymentMethod", event.getPaymentMethod() != null ? event.getPaymentMethod().name() : null);
        payload.put("createdAt", java.time.Instant.now().toString());
        return payload;
    }

    /**
     * OrderPaidEvent를 Kafka 이벤트 페이로드로 변환
     */
    private Map<String, Object> createOrderPaidPayload(OrderEvent.OrderPaidEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "OrderPaid");
        payload.put("aggregateId", event.getOrderId().toString());
        payload.put("orderId", event.getOrderId());
        payload.put("orderPublicId", event.getOrderPublicId());
        payload.put("userId", event.getUserId());
        payload.put("couponId", event.getCouponId());
        payload.put("orderStatus", event.getOrderStatus() != null ? event.getOrderStatus().name() : null);
        payload.put("paymentMethod", event.getPaymentMethod() != null ? event.getPaymentMethod().name() : null);
        payload.put("transactionKey", event.getTransactionKey());
        payload.put("finalPrice", event.getFinalPrice() != null ? event.getFinalPrice().amount() : null);
        payload.put("statusMessage", event.getStatusMessage());
        payload.put("createdAt", java.time.Instant.now().toString());
        return payload;
    }
}

