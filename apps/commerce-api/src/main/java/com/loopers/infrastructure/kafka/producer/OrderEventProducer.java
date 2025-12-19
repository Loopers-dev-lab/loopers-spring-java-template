package com.loopers.infrastructure.kafka.producer;

import com.loopers.infrastructure.kafka.dto.OrderEventDto;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${kafka.topic.order-events-name}")
    private String orderEventsTopic;

    @Retry(name = "kafkaProducer", fallbackMethod = "orderEventFallback")
    public void sendOrderCreatedEvent(
            Long orderId,
            Long userId,
            Long totalAmount,
            Long discountAmount,
            List<OrderEventDto.OrderItemDto> items
    ) {
        OrderEventDto event = OrderEventDto.created(orderId, userId, totalAmount, discountAmount, items);
        kafkaTemplate.send(orderEventsTopic, orderId.toString(), event);
        log.info("주문 생성 이벤트 발행: orderId={}, userId={}", orderId, userId);
    }

    @Retry(name = "kafkaProducer", fallbackMethod = "orderStatusEventFallback")
    public void sendOrderCompletedEvent(Long orderId, Long userId) {
        OrderEventDto event = OrderEventDto.completed(orderId, userId);
        kafkaTemplate.send(orderEventsTopic, orderId.toString(), event);
        log.info("주문 완료 이벤트 발행: orderId={}, userId={}", orderId, userId);
    }

    @Retry(name = "kafkaProducer", fallbackMethod = "orderStatusEventFallback")
    public void sendOrderFailedEvent(Long orderId, Long userId) {
        OrderEventDto event = OrderEventDto.failed(orderId, userId);
        kafkaTemplate.send(orderEventsTopic, orderId.toString(), event);
        log.info("주문 실패 이벤트 발행: orderId={}, userId={}", orderId, userId);
    }

    public void orderEventFallback(
            Long orderId,
            Long userId,
            Long totalAmount,
            Long discountAmount,
            List<OrderEventDto.OrderItemDto> items,
            Throwable ex
    ) {
        log.error("주문 생성 이벤트 발행 실패 (재시도 후): orderId={}, userId={}",
                orderId, userId, ex);
    }

    public void orderStatusEventFallback(Long orderId, Long userId, Throwable ex) {
        log.error("주문 상태 이벤트 발행 실패 (재시도 후): orderId={}, userId={}",
                orderId, userId, ex);
    }
}
