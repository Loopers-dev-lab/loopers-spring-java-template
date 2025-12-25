package com.loopers.infrastructure.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import com.loopers.infrastructure.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogEventConsumerTest {

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Mock
    private ProductMetricsRepository productMetricsRepository;

    @Mock
    private ProductCacheService productCacheService;

    @Mock
    private Acknowledgment acknowledgment;

    private CatalogEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new CatalogEventConsumer(eventHandledRepository, productMetricsRepository, productCacheService, objectMapper);
    }

    @Test
    @DisplayName("좋아요 이벤트 수신 시 ProductMetrics의 likeCount를 증가시킨다")
    void consumeTest1() {
        String payload = "{\"productId\":1,\"userId\":100,\"liked\":true,\"occurredAt\":\"2024-01-01T10:00:00\"}";
        ConsumerRecord<String, String> record = createRecord("1", payload, "100");
        when(eventHandledRepository.existsByEventId("100")).thenReturn(false);
        when(productMetricsRepository.findByProductId(1L)).thenReturn(Optional.empty());

        consumer.consume(record, acknowledgment);

        verify(eventHandledRepository).save(argThat(e -> e.getEventId().equals("100")));
        verify(productMetricsRepository).save(argThat(m -> m.getProductId().equals(1L) && m.getLikeCount() == 1L));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("좋아요 취소 이벤트 수신 시 ProductMetrics의 likeCount를 감소시킨다")
    void consumeTest2() {
        String payload = "{\"productId\":1,\"userId\":100,\"liked\":false,\"occurredAt\":\"2024-01-01T12:00:00\"}";
        ConsumerRecord<String, String> record = createRecord("1", payload, "101");
        ProductMetrics existingMetrics = ProductMetrics.create(1L);
        existingMetrics.updateLikeIfNewer(true, java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
        existingMetrics.updateLikeIfNewer(true, java.time.LocalDateTime.of(2024, 1, 1, 11, 0));

        when(eventHandledRepository.existsByEventId("101")).thenReturn(false);
        when(productMetricsRepository.findByProductId(1L)).thenReturn(Optional.of(existingMetrics));

        consumer.consume(record, acknowledgment);

        verify(productMetricsRepository).save(argThat(m -> m.getLikeCount() == 1L));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 무시하고 ack만 한다")
    void consumeTest3() {
        String payload = "{\"productId\":1,\"userId\":100,\"liked\":true}";
        ConsumerRecord<String, String> record = createRecord("1", payload, "100");
        when(eventHandledRepository.existsByEventId("100")).thenReturn(true);

        consumer.consume(record, acknowledgment);

        verify(productMetricsRepository, never()).save(any());
        verify(eventHandledRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("이전 이벤트보다 오래된 이벤트는 무시한다")
    void consumeTest4() {
        String newPayload = "{\"productId\":1,\"userId\":100,\"liked\":true,\"occurredAt\":\"2024-01-01T11:00:00\"}";
        String oldPayload = "{\"productId\":1,\"userId\":100,\"liked\":true,\"occurredAt\":\"2024-01-01T10:00:00\"}";
        ConsumerRecord<String, String> newRecord = createRecord("1", newPayload, "100");
        ConsumerRecord<String, String> oldRecord = createRecord("1", oldPayload, "101");

        ProductMetrics metrics = ProductMetrics.create(1L);
        metrics.updateLikeIfNewer(true, java.time.LocalDateTime.of(2024, 1, 1, 11, 0));

        when(eventHandledRepository.existsByEventId("100")).thenReturn(false);
        when(eventHandledRepository.existsByEventId("101")).thenReturn(false);
        when(productMetricsRepository.findByProductId(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(metrics));

        consumer.consume(newRecord, acknowledgment);
        consumer.consume(oldRecord, acknowledgment);

        verify(productMetricsRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("재고 소진 이벤트 수신 시 상품 캐시를 무효화한다")
    void consumeTest5() {
        String payload = "{\"productId\":1,\"occurredAt\":\"2024-01-01T10:00:00\"}";
        ConsumerRecord<String, String> record = createRecord("1", payload, "200", "StockDepletedEvent");
        when(eventHandledRepository.existsByEventId("200")).thenReturn(false);

        consumer.consume(record, acknowledgment);

        verify(productCacheService).deleteProductDetail(1L);
        verify(productCacheService).invalidateProductListCaches();
        verify(eventHandledRepository).save(argThat(e -> e.getEventId().equals("200")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("동일 메시지 재전송 시 최종 결과는 한 번만 반영된다")
    void consumeTest6() {
        String payload = "{\"productId\":1,\"userId\":100,\"liked\":true,\"occurredAt\":\"2024-01-01T10:00:00\"}";
        ConsumerRecord<String, String> firstRecord = createRecord("1:100", payload, "300");
        ConsumerRecord<String, String> duplicateRecord = createRecord("1:100", payload, "300");

        when(eventHandledRepository.existsByEventId("300"))
                .thenReturn(false)
                .thenReturn(true);
        when(productMetricsRepository.findByProductId(1L)).thenReturn(Optional.empty());

        consumer.consume(firstRecord, acknowledgment);
        consumer.consume(duplicateRecord, acknowledgment);

        verify(eventHandledRepository, times(1)).save(any());
        verify(productMetricsRepository, times(1)).save(argThat(m ->
                m.getProductId().equals(1L) && m.getLikeCount() == 1L));
        verify(acknowledgment, times(2)).acknowledge();
    }

    private ConsumerRecord<String, String> createRecord(String key, String value, String outboxId) {
        return createRecord(key, value, outboxId, "ProductLikedEvent");
    }

    private ConsumerRecord<String, String> createRecord(String key, String value, String outboxId, String eventType) {
        RecordHeaders headers = new RecordHeaders();
        headers.add("outbox-id", outboxId.getBytes(StandardCharsets.UTF_8));
        headers.add("event-type", eventType.getBytes(StandardCharsets.UTF_8));
        return new ConsumerRecord<>("catalog-events", 0, 0, 0L, null, 0, 0, key, value, headers, Optional.empty());
    }
}
