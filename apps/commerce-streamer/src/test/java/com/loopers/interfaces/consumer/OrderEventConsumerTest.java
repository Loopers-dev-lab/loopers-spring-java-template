package com.loopers.interfaces.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.inbox.EventInboxService;
import com.loopers.application.metrics.ProductMetricsService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * OrderEventConsumer 단위 테스트
 *
 * 목적:
 * 1. Consumer가 주문/결제 이벤트를 올바르게 처리하는지 검증
 * 2. Inbox 패턴으로 중복 이벤트를 방지하는지 검증
 * 3. 주문 데이터를 ProductMetrics에 올바르게 반영하는지 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order 이벤트 Consumer")
class OrderEventConsumerTest {

    @Mock
    private EventInboxService eventInboxService;

    @Mock
    private ProductMetricsService productMetricsService;

    private OrderEventConsumer orderEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // ObjectMapper는 실제 인스턴스 사용, DeadLetterQueueService는 null (단위 테스트에서 불필요)
        orderEventConsumer = new OrderEventConsumer(
            eventInboxService,
            productMetricsService,
            null,  // DeadLetterQueueService - 단위 테스트에서 사용하지 않음
            objectMapper
        );
    }

    @Nested
    @DisplayName("OrderCreatedEvent 처리")
    class OrderCreatedEventTest {

        @Test
        @DisplayName("주문 생성 이벤트를 받으면 ProductMetrics의 주문 수와 판매 금액을 증가시킨다")
        void incrementOrderCountAndSalesAmount() throws Exception {
            // Given: 주문 생성 이벤트
            Long productId = 1L;
            int quantity = 3;
            BigDecimal totalAmount = new BigDecimal("30000");

            String eventId = "order-event-001";
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("productId", productId);
            orderData.put("quantity", quantity);
            orderData.put("totalAmount", totalAmount);

            Map<String, Object> event = createEvent(eventId, "OrderCreatedEvent", "ORDER", "order-123");
            event.put("payload", orderData);

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = orderEventConsumer.processEvent(record);

            // Then: Inbox에 저장하고, 주문 수와 판매 금액 증가
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "ORDER", "order-123", "OrderCreatedEvent");
            verify(productMetricsService).incrementOrderCount(eq(productId), eq(quantity), eq(totalAmount));
        }

        @Test
        @DisplayName("payload가 String 타입일 때도 정상적으로 파싱하여 처리한다")
        void handleStringPayload() throws Exception {
            // Given: payload가 JSON String인 경우
            Long productId = 2L;
            int quantity = 5;
            BigDecimal totalAmount = new BigDecimal("50000");

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("productId", productId);
            orderData.put("quantity", quantity);
            orderData.put("totalAmount", totalAmount);

            String orderDataJson = objectMapper.writeValueAsString(orderData);

            String eventId = "order-event-002";
            Map<String, Object> event = createEvent(eventId, "OrderCreatedEvent", "ORDER", "order-456");
            event.put("payload", orderDataJson);  // String으로 저장

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = orderEventConsumer.processEvent(record);

            // Then: String payload도 파싱하여 정상 처리
            assertThat(result).isTrue();
            verify(productMetricsService).incrementOrderCount(eq(productId), eq(quantity), eq(totalAmount));
        }

        @Test
        @DisplayName("중복된 주문 이벤트는 무시한다 (멱등성 보장)")
        void ignoreDuplicateOrderEvent() throws Exception {
            // Given: 이미 처리된 주문 이벤트
            String eventId = "order-event-001";
            Map<String, Object> event = createEvent(eventId, "OrderCreatedEvent", "ORDER", "order-123");
            event.put("payload", new HashMap<>());

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(true);

            // When: 같은 이벤트를 다시 수신
            boolean result = orderEventConsumer.processEvent(record);

            // Then: 처리하지 않고 스킵
            assertThat(result).isFalse();
            verify(productMetricsService, never()).incrementOrderCount(any(Long.class), anyInt(), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("PaymentSuccessEvent 처리")
    class PaymentSuccessEventTest {

        @Test
        @DisplayName("결제 성공 이벤트를 받으면 Inbox에 저장한다 (추가 처리 없음)")
        void savePaymentSuccessEvent() throws Exception {
            // Given: 결제 성공 이벤트
            String eventId = "payment-event-001";
            Map<String, Object> event = createEvent(eventId, "PaymentSuccessEvent", "PAYMENT", "payment-123");

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = orderEventConsumer.processEvent(record);

            // Then: Inbox에만 저장 (현재는 추가 비즈니스 로직 없음)
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "PAYMENT", "payment-123", "PaymentSuccessEvent");
            verify(productMetricsService, never()).incrementOrderCount(any(Long.class), anyInt(), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("예외 상황 처리")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("payload가 없는 OrderCreatedEvent는 경고 로그만 출력하고 처리를 완료한다")
        void handleMissingPayload() throws Exception {
            // Given: payload가 없는 주문 이벤트
            String eventId = "order-event-999";
            Map<String, Object> event = createEvent(eventId, "OrderCreatedEvent", "ORDER", "order-999");
            // payload 없음

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = orderEventConsumer.processEvent(record);

            // Then: Inbox에는 저장하지만, ProductMetrics는 업데이트하지 않음
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "ORDER", "order-999", "OrderCreatedEvent");
            verify(productMetricsService, never()).incrementOrderCount(any(Long.class), anyInt(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("필수 필드가 누락된 OrderCreatedEvent는 경고 로그만 출력한다")
        void handleIncompleteOrderData() throws Exception {
            // Given: productId가 없는 주문 데이터
            String eventId = "order-event-incomplete";
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("quantity", 1);
            orderData.put("totalAmount", 10000);
            // productId 없음

            Map<String, Object> event = createEvent(eventId, "OrderCreatedEvent", "ORDER", "order-incomplete");
            event.put("payload", orderData);

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = orderEventConsumer.processEvent(record);

            // Then: Inbox에는 저장하지만, 필수 필드 누락으로 ProductMetrics는 업데이트하지 않음
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "ORDER", "order-incomplete", "OrderCreatedEvent");
            verify(productMetricsService, never()).incrementOrderCount(any(Long.class), anyInt(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("eventId가 없는 메시지는 false를 반환하고 처리하지 않는다")
        void handleMissingEventId() throws Exception {
            // Given: eventId가 없는 잘못된 메시지
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "OrderCreatedEvent");
            event.put("aggregateType", "ORDER");
            event.put("aggregateId", "order-123");
            // id 필드 없음

            ConsumerRecord<Object, Object> record = createConsumerRecord("order-events", event);

            // When: Consumer가 이벤트 처리 시도
            boolean result = orderEventConsumer.processEvent(record);

            // Then: 처리하지 않고 false 반환
            assertThat(result).isFalse();
            verify(eventInboxService, never()).isDuplicate(any());
            verify(eventInboxService, never()).save(any(), any(), any(), any());
        }
    }

    // Helper methods
    private Map<String, Object> createEvent(String id, String eventType, String aggregateType, String aggregateId) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("eventType", eventType);
        event.put("aggregateType", aggregateType);
        event.put("aggregateId", aggregateId);
        return event;
    }

    private ConsumerRecord<Object, Object> createConsumerRecord(String topic, Map<String, Object> event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        return new ConsumerRecord<>(topic, 0, 0L, "key", payload);
    }
}
