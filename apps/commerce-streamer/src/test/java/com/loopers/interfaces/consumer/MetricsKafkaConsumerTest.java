package com.loopers.interfaces.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.metrics.MetricsService;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.EventDeserializer;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;

/**
 * MetricsKafkaConsumer 멱등성 및 신뢰성 테스트
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@ExtendWith(MockitoExtension.class)
class MetricsKafkaConsumerTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private EventDeserializer eventDeserializer;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private MetricsKafkaConsumer consumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("중복된 이벤트 ID는 한 번만 처리되어야 한다")
    void shouldProcessEventOnlyOnce() {
        // Given
        String eventId = "test-event-123";
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                "{\"productId\":1,\"userId\":100}"
        );

        ProductViewPayloadV1 payload = new ProductViewPayloadV1(1L, 100L);

        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("catalog-events", 0, 0, null, envelope);

        // 첫 번째 호출에서는 true (처음 처리), 두 번째 호출에서는 false (이미 처리됨)
        when(metricsService.tryMarkHandled(eventId))
                .thenReturn(true)
                .thenReturn(false);

        when(eventDeserializer.deserializeEnvelope(envelope))
                .thenReturn(envelope);

        when(eventDeserializer.deserializeProductView(envelope.payloadJson()))
                .thenReturn(payload);

        // When - 같은 이벤트를 두 번 처리
        consumer.onCatalogEvents(List.of(record, record), acknowledgment);

        // Then - 비즈니스 로직은 한 번만 호출되어야 함
        verify(metricsService, times(2)).tryMarkHandled(eventId);
        verify(metricsService, times(1)).incrementView(eq(1L), anyLong());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("잘못된 이벤트 봉투는 무시되어야 한다")
    void shouldIgnoreInvalidEventEnvelope() {
        // Given
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("catalog-events", 0, 0, null, "invalid-json");

        when(eventDeserializer.deserializeEnvelope("invalid-json"))
                .thenReturn(null);

        // When
        consumer.onCatalogEvents(List.of(record), acknowledgment);

        // Then
        verify(metricsService, never()).tryMarkHandled(anyString());
        verify(metricsService, never()).incrementView(anyLong(), anyLong());
        verify(acknowledgment, times(1)).acknowledge(); // 배치는 여전히 ack 되어야 함
    }

    @Test
    @DisplayName("PAYMENT_SUCCESS 이벤트가 상품별로 개별 처리되어야 한다")
    void shouldProcessPaymentSuccessEvent() {
        // Given
        String eventId = "payment-event-456";
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "PAYMENT_SUCCESS",
                "v1",
                System.currentTimeMillis(),
                "{\"orderId\":12345,\"orderNumber\":67890,\"userId\":100,\"productId\":1,\"quantity\":2,\"unitPrice\":1000,\"totalPrice\":2000}"
        );

        // 새로운 PaymentSuccessPayloadV1 구조 (상품별 개별 이벤트)
        PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(
                12345L,    // orderId
                67890L,    // orderNumber
                100L,      // userId
                1L,        // productId
                2,         // quantity
                java.math.BigDecimal.valueOf(1000), // unitPrice
                java.math.BigDecimal.valueOf(2000)  // totalPrice
        );

        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("order-events", 0, 0, null, envelope);

        when(metricsService.tryMarkHandled(eventId)).thenReturn(true);
        when(eventDeserializer.deserializeEnvelope(envelope)).thenReturn(envelope);
        when(eventDeserializer.deserializePaymentSuccess(envelope.payloadJson())).thenReturn(payload);

        // When
        consumer.onOrderEvents(List.of(record), acknowledgment);

        // Then
        verify(metricsService, times(1)).tryMarkHandled(eventId);
        verify(metricsService, times(1)).addSales(eq(1L), eq(2), anyLong());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("개별 메시지 처리 실패가 전체 배치를 실패시키지 않아야 한다")
    void shouldContinueProcessingWhenIndividualMessageFails() {
        // Given
        String validEventId = "valid-event";
        String invalidEventId = "invalid-event";

        DomainEventEnvelope validEnvelope = new DomainEventEnvelope(
                validEventId,
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                "{\"productId\":1,\"userId\":100}"
        );

        DomainEventEnvelope invalidEnvelope = new DomainEventEnvelope(
                invalidEventId,
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                "invalid-payload"
        );

        ProductViewPayloadV1 validPayload = new ProductViewPayloadV1(1L, 100L);

        ConsumerRecord<Object, Object> validRecord = new ConsumerRecord<>("catalog-events", 0, 0, null, validEnvelope);
        ConsumerRecord<Object, Object> invalidRecord = new ConsumerRecord<>("catalog-events", 0, 1, null, invalidEnvelope);

        when(metricsService.tryMarkHandled(validEventId)).thenReturn(true);
        when(metricsService.tryMarkHandled(invalidEventId)).thenReturn(true);

        when(eventDeserializer.deserializeEnvelope(validEnvelope)).thenReturn(validEnvelope);
        when(eventDeserializer.deserializeEnvelope(invalidEnvelope)).thenReturn(invalidEnvelope);

        when(eventDeserializer.deserializeProductView(validEnvelope.payloadJson())).thenReturn(validPayload);
        when(eventDeserializer.deserializeProductView(invalidEnvelope.payloadJson())).thenReturn(null); // 파싱 실패

        // When
        consumer.onCatalogEvents(List.of(validRecord, invalidRecord), acknowledgment);

        // Then - 유효한 메시지는 처리되고, 전체 배치는 ack 되어야 함
        verify(metricsService, times(1)).incrementView(eq(1L), anyLong());
        verify(acknowledgment, times(1)).acknowledge();
    }
}
