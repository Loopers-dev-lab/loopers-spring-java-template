package com.loopers.interfaces.consumer.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.order.OrderEventHandler;
import com.loopers.interfaces.consumer.order.dto.OrderEvent;
import com.loopers.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// 메시지 단건 처리 비활성화
//@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventHandler orderEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.ORDER,
            groupId = "commerce-collector-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("주문 이벤트 수신 - key: {}, message: {}", key, message);

            // DTO로 역직렬화
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);

            // 이벤트 타입별 처리
            if (KafkaTopics.Order.ORDER_CREATED.equals(event.eventType())) {
                handleOrderCreatedEvent(event);
            }

            // 수동 커밋
            acknowledgment.acknowledge();
            log.info("주문 이벤트 처리 완료 - eventId: {}", event.eventId());

        } catch (JsonProcessingException e) {
            // JSON 파싱 에러 - 재시도 불필요
            log.error("JSON 파싱 실패 (재시도 안 함) - message: {}", message, e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Business 로직 에러 - 재시도
            log.error("이벤트 처리 실패 (재시도) - message: {}", message, e);
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }

    /**
     * ORDER_CREATED 이벤트 처리
     * 주문 내 각 상품별로 집계 처리
     */
    private void handleOrderCreatedEvent(OrderEvent event) {
        OrderEvent.OrderCreatedPayload payload = event.payload();

        // Payload 검증
        if (payload == null || payload.items() == null) {
            log.error("잘못된 ORDER_CREATED 형식 - payload 또는 items 누락 - eventId: {}", event.eventId());
            return;  // 재시도 방지
        }

        // 주문 내 각 상품별로 처리
        for (OrderEvent.OrderCreatedPayload.OrderItem item : payload.items()) {
            // 상품별 필드 검증
            if (item.productId() == null || item.quantity() == null) {
                log.error("잘못된 OrderItem 형식 - eventId: {}, item: {}", event.eventId(), item);
                continue;  // 해당 상품만 스킵하고 다음 상품 처리
            }

            // 상품별 고유 eventId 생성 (멱등성 보장)
            String itemEventId = event.eventId() + "-" + item.productId();

            orderEventHandler.handleOrderCreated(itemEventId, item.productId(), item.quantity());
        }
    }
}
