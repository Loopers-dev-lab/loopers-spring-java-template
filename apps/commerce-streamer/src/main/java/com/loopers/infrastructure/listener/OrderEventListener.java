package com.loopers.infrastructure.listener;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.infrastructure.dlq.DlqService;
import com.loopers.infrastructure.idempotency.IdempotencyService;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final IdempotencyService idempotencyService;
    private final ProductMetricsService productMetricsService;
    private final OrderRepository orderRepository;
    private final DlqService dlqService;
    private final ObjectMapper objectMapper;
    
    // 메시지별 재시도 횟수 추적 (메모리 기반, 재시작 시 초기화됨)
    // 프로덕션에서는 Redis 등을 사용하는 것을 권장
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();
    
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * order-events 토픽에서 메시지를 수신하여 처리
     * 
     * 처리 플로우:
     * 1. 메시지 수신
     * 2. event_id 추출
     * 3. 멱등성 체크 (event_handled 테이블)
     * 4. 비즈니스 로직 수행 (주문 완료 시 판매량 집계 등)
     * 5. event_handled에 기록 (같은 트랜잭션)
     * 6. 트랜잭션 커밋 후 Ack
     */
    @KafkaListener(
            topics = "order-events",
            groupId = "commerce-metrics-consumer-group",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    @Transactional
    public void handleOrderEvents(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received {} messages from order-events", records.size());

        for (ConsumerRecord<Object, Object> record : records) {
            try {
                String key = record.key() != null ? record.key().toString() : null;
                Object value = record.value();
                
                // JSON 문자열을 Map으로 파싱
                Map<String, Object> message = parseMessage(value);
                
                // 이벤트 정보 추출
                String eventId = extractEventId(message);
                String eventType = extractEventType(message);
                String aggregateId = key != null ? key : extractAggregateId(message);

                if (eventId == null || eventType == null || aggregateId == null) {
                    log.warn("Missing required fields in message: eventId={}, eventType={}, aggregateId={}", 
                            eventId, eventType, aggregateId);
                    continue;
                }

                // 멱등성 체크
                if (idempotencyService.isAlreadyHandled(eventId)) {
                    log.warn("Event already handled, skipping: eventId={}, eventType={}", eventId, eventType);
                    continue;
                }

                // 이벤트 타입별 처리
                handleEvent(eventType, message);

                // 처리 완료 기록 (비즈니스 로직과 같은 트랜잭션)
                idempotencyService.markAsHandled(
                        eventId,
                        eventType,
                        aggregateId,
                        "OrderEventListener"
                );

                log.debug("Event processed successfully: eventId={}, eventType={}, aggregateId={}", 
                        eventId, eventType, aggregateId);

            } catch (Exception e) {
                String messageKey = record.key() != null ? record.key().toString() : 
                        extractEventId(parseMessage(record.value()));
                int retryCount = retryCountMap.getOrDefault(messageKey, 0) + 1;
                retryCountMap.put(messageKey, retryCount);
                
                log.error("Failed to process message: topic={}, key={}, offset={}, retryCount={}", 
                        record.topic(), record.key(), record.offset(), retryCount, e);
                
                // 재시도 횟수 초과 시 DLQ로 전송하고 Ack
                if (retryCount >= MAX_RETRY_COUNT) {
                    try {
                        dlqService.sendToDlq(
                                record.topic(),
                                record.key(),
                                record.value(),
                                e.getMessage(),
                                retryCount
                        );
                        retryCountMap.remove(messageKey); // 재시도 카운트 제거
                        log.warn("Message sent to DLQ after {} retries: topic={}, key={}", 
                                retryCount, record.topic(), record.key());
                        // DLQ로 전송했으므로 이 메시지는 건너뛰고 계속 진행
                        continue;
                    } catch (Exception dlqException) {
                        log.error("Failed to send message to DLQ: topic={}, key={}", 
                                record.topic(), record.key(), dlqException);
                        // DLQ 전송 실패 시 예외를 다시 던져서 재시도 유도
                        throw e;
                    }
                } else {
                    // 재시도 가능한 경우 예외를 던져서 트랜잭션 롤백 및 재시도 유도
                    throw e;
                }
            }
        }

        // 모든 메시지 처리 완료 후 Ack
        acknowledgment.acknowledge();
        log.debug("Acknowledged {} messages", records.size());
    }

    private void handleEvent(String eventType, Map<String, Object> message) {
        switch (eventType) {
            case "OrderCreated":
                // 주문 생성 이벤트 처리 (필요시)
                log.debug("Order created event received");
                break;
            case "OrderPaid":
            case "OrderCompleted":
                // 주문 완료 시 판매량 집계
                handleOrderPaidEvent(message);
                break;
            default:
                log.warn("Unknown event type: {}", eventType);
        }
    }
    
    /**
     * OrderPaid 이벤트 처리
     * 주문 항목별로 상품의 판매량(soldCount)을 증가시킵니다.
     * 
     * 주의: OrderPaidEvent에는 orderItems가 포함되어 있지 않으므로,
     * orderId를 사용하여 Order 엔티티를 조회하여 orderItems를 가져옵니다.
     */
    private void handleOrderPaidEvent(Map<String, Object> message) {
        try {
            // orderId 추출
            Object orderIdObj = message.get("orderId");
            if (orderIdObj == null) {
                log.warn("OrderPaid event missing orderId: {}", message);
                return;
            }
            
            Long orderId;
            try {
                orderId = Long.parseLong(orderIdObj.toString());
            } catch (NumberFormatException e) {
                log.error("Failed to parse orderId: {}", orderIdObj, e);
                return;
            }
            
            // Order 엔티티 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, 
                            "주문을 찾을 수 없습니다: orderId=" + orderId));
            
            // 각 주문 항목에 대해 판매량 증가
            order.getOrderItems().forEach(item -> {
                Long productId = item.getProductId();
                Integer quantity = item.getQuantity();
                
                if (productId == null || quantity == null || quantity <= 0) {
                    log.warn("Invalid OrderItem: productId={}, quantity={}", productId, quantity);
                    return;
                }
                
                try {
                    productMetricsService.incrementSoldCount(productId, Long.valueOf(quantity));
                    log.debug("Incremented sold count for product: productId={}, quantity={}", 
                            productId, quantity);
                } catch (Exception e) {
                    log.error("Failed to increment sold count: productId={}, quantity={}", 
                            productId, quantity, e);
                    throw e; // 트랜잭션 롤백을 위해 예외 재발생
                }
            });
        } catch (Exception e) {
            log.error("Failed to handle OrderPaid event: {}", message, e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value instanceof String) {
            try {
                return objectMapper.readValue((String) value, Map.class);
            } catch (Exception e) {
                log.error("Failed to parse message as JSON", e);
                throw new RuntimeException("Failed to parse message", e);
            }
        }
        throw new IllegalArgumentException("Unsupported message type: " + value.getClass());
    }

    private String extractEventId(Map<String, Object> message) {
        if (message.containsKey("eventId")) {
            return message.get("eventId").toString();
        }
        if (message.containsKey("id")) {
            return message.get("id").toString();
        }
        return null;
    }

    private String extractEventType(Map<String, Object> message) {
        if (message.containsKey("eventType")) {
            return message.get("eventType").toString();
        }
        if (message.containsKey("type")) {
            return message.get("type").toString();
        }
        return null;
    }

    private String extractAggregateId(Map<String, Object> message) {
        if (message.containsKey("aggregateId")) {
            return message.get("aggregateId").toString();
        }
        if (message.containsKey("orderId")) {
            return message.get("orderId").toString();
        }
        return null;
    }
}

