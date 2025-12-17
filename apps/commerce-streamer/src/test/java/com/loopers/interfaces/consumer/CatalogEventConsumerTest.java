package com.loopers.interfaces.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.inbox.EventInboxService;
import com.loopers.application.metrics.ProductMetricsService;
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
 * CatalogEventConsumer 단위 테스트
 *
 * 목적:
 * 1. Consumer가 각 이벤트 타입을 올바르게 처리하는지 검증
 * 2. Inbox 패턴으로 중복 이벤트를 방지하는지 검증
 * 3. 비즈니스 로직(ProductMetricsService)이 올바르게 호출되는지 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Catalog 이벤트 Consumer")
class CatalogEventConsumerTest {

    @Mock
    private EventInboxService eventInboxService;

    @Mock
    private ProductMetricsService productMetricsService;

    private CatalogEventConsumer catalogEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // ObjectMapper는 실제 인스턴스 사용, DeadLetterQueueService는 null (단위 테스트에서 불필요)
        catalogEventConsumer = new CatalogEventConsumer(
            eventInboxService,
            productMetricsService,
            null,  // DeadLetterQueueService - 단위 테스트에서 사용하지 않음
            objectMapper
        );
    }

    @Nested
    @DisplayName("LikeCreatedEvent 처리")
    class LikeCreatedEventTest {

        @Test
        @DisplayName("좋아요 생성 이벤트를 받으면 ProductMetrics의 좋아요 수를 증가시킨다")
        void incrementLikeCount() throws Exception {
            // Given: 좋아요 생성 이벤트
            Long productId = 1L;
            String eventId = "event-001";
            Map<String, Object> event = createEvent(eventId, "LikeCreatedEvent", "LIKE", productId.toString());
            ConsumerRecord<Object, Object> record = createConsumerRecord("catalog-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = catalogEventConsumer.processEvent(record);

            // Then: Inbox에 저장하고, 좋아요 수 증가
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "LIKE", productId.toString(), "LikeCreatedEvent");
            verify(productMetricsService).incrementLikeCount(productId);
        }

        @Test
        @DisplayName("중복된 좋아요 이벤트는 무시한다 (멱등성 보장)")
        void ignoreDuplicateEvent() throws Exception {
            // Given: 이미 처리된 이벤트 (Inbox에 존재)
            String eventId = "event-001";
            Map<String, Object> event = createEvent(eventId, "LikeCreatedEvent", "LIKE", "1");
            ConsumerRecord<Object, Object> record = createConsumerRecord("catalog-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(true);

            // When: 같은 이벤트를 다시 수신
            boolean result = catalogEventConsumer.processEvent(record);

            // Then: 처리하지 않고 스킵 (false 반환)
            assertThat(result).isFalse();
            verify(eventInboxService, never()).save(any(), any(), any(), any());
            verify(productMetricsService, never()).incrementLikeCount(any());
        }
    }

    @Nested
    @DisplayName("LikeDeletedEvent 처리")
    class LikeDeletedEventTest {

        @Test
        @DisplayName("좋아요 삭제 이벤트를 받으면 ProductMetrics의 좋아요 수를 감소시킨다")
        void decrementLikeCount() throws Exception {
            // Given: 좋아요 삭제 이벤트
            Long productId = 2L;
            String eventId = "event-002";
            Map<String, Object> event = createEvent(eventId, "LikeDeletedEvent", "LIKE", productId.toString());
            ConsumerRecord<Object, Object> record = createConsumerRecord("catalog-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = catalogEventConsumer.processEvent(record);

            // Then: Inbox에 저장하고, 좋아요 수 감소
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "LIKE", productId.toString(), "LikeDeletedEvent");
            verify(productMetricsService).decrementLikeCount(productId);
        }
    }

    @Nested
    @DisplayName("ProductViewedEvent 처리")
    class ProductViewedEventTest {

        @Test
        @DisplayName("상품 조회 이벤트를 받으면 ProductMetrics의 조회 수를 증가시킨다")
        void incrementViewCount() throws Exception {
            // Given: 상품 조회 이벤트
            Long productId = 3L;
            String eventId = "event-003";
            Map<String, Object> event = createEvent(eventId, "ProductViewedEvent", "PRODUCT", productId.toString());
            ConsumerRecord<Object, Object> record = createConsumerRecord("catalog-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = catalogEventConsumer.processEvent(record);

            // Then: Inbox에 저장하고, 조회 수 증가
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "PRODUCT", productId.toString(), "ProductViewedEvent");
            verify(productMetricsService).incrementViewCount(productId);
        }
    }

    @Nested
    @DisplayName("예외 상황 처리")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("eventId가 없는 메시지는 false를 반환하고 처리하지 않는다")
        void handleMissingEventId() throws Exception {
            // Given: eventId가 없는 잘못된 메시지
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "LikeCreatedEvent");
            event.put("aggregateType", "LIKE");
            event.put("aggregateId", "1");
            // id 필드 없음

            ConsumerRecord<Object, Object> record = createConsumerRecord("catalog-events", event);

            // When: Consumer가 이벤트 처리 시도
            boolean result = catalogEventConsumer.processEvent(record);

            // Then: 처리하지 않고 false 반환
            assertThat(result).isFalse();
            verify(eventInboxService, never()).isDuplicate(any());
            verify(eventInboxService, never()).save(any(), any(), any(), any());
            verify(productMetricsService, never()).incrementLikeCount(any());
        }

        @Test
        @DisplayName("알 수 없는 이벤트 타입은 Inbox에만 저장하고 비즈니스 로직은 실행하지 않는다")
        void handleUnknownEventType() throws Exception {
            // Given: 알 수 없는 이벤트 타입
            String eventId = "event-999";
            Map<String, Object> event = createEvent(eventId, "UnknownEvent", "UNKNOWN", "1");
            ConsumerRecord<Object, Object> record = createConsumerRecord("catalog-events", event);

            when(eventInboxService.isDuplicate(eventId)).thenReturn(false);

            // When: Consumer가 이벤트 처리
            boolean result = catalogEventConsumer.processEvent(record);

            // Then: Inbox에는 저장하지만, ProductMetrics는 업데이트하지 않음
            assertThat(result).isTrue();
            verify(eventInboxService).save(eventId, "UNKNOWN", "1", "UnknownEvent");
            verify(productMetricsService, never()).incrementLikeCount(any());
            verify(productMetricsService, never()).decrementLikeCount(any());
            verify(productMetricsService, never()).incrementViewCount(any());
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
