package com.loopers.interfaces.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.loopers.application.event.EventProcessingFacade;
import com.loopers.application.event.dto.EventProcessingResult.CatalogEventResult;
import com.loopers.application.event.dto.EventProcessingResult.OrderEventResult;
import com.loopers.cache.dto.CachePayloads.RankingScore;
import com.loopers.infrastructure.event.DomainEventEnvelope;

/**
 * MetricsKafkaConsumer 멱등성 및 신뢰성 테스트
 * <p>
 * Consumer는 EventProcessingFacade에 위임만 하므로, Facade mock을 통해 테스트
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@ExtendWith(MockitoExtension.class)
class MetricsKafkaConsumerTest {

    @Mock
    private EventProcessingFacade eventProcessingFacade;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private MetricsKafkaConsumer consumer;

    @Test
    @DisplayName("카탈로그 이벤트가 Facade를 통해 처리되어야 한다")
    void shouldProcessCatalogEventThroughFacade() {
        // Given
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                "test-event-123",
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                "{\"productId\":1,\"userId\":100}"
        );

        RankingScore rankingScore = RankingScore.forProductView(1L, System.currentTimeMillis());
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("catalog-events", 0, 0, null, envelope);

        when(eventProcessingFacade.processCatalogEvent(envelope))
                .thenReturn(CatalogEventResult.processed(rankingScore));

        // When
        consumer.onCatalogEvents(List.of(record), acknowledgment);

        // Then
        verify(eventProcessingFacade, times(1)).processCatalogEvent(envelope);
        verify(eventProcessingFacade, times(1)).updateRankingScores(anyList(), isNull());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("처리되지 않은 이벤트는 랭킹 업데이트에 포함되지 않아야 한다")
    void shouldNotIncludeUnprocessedEventsInRankingUpdate() {
        // Given
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                "test-event-456",
                "UNKNOWN_EVENT",
                "v1",
                System.currentTimeMillis(),
                "{}"
        );

        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("catalog-events", 0, 0, null, envelope);

        when(eventProcessingFacade.processCatalogEvent(envelope))
                .thenReturn(CatalogEventResult.notProcessed());

        // When
        consumer.onCatalogEvents(List.of(record), acknowledgment);

        // Then
        verify(eventProcessingFacade, times(1)).processCatalogEvent(envelope);
        verify(eventProcessingFacade, never()).updateRankingScores(anyList(), any());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("PAYMENT_SUCCESS 이벤트가 Facade를 통해 처리되어야 한다")
    void shouldProcessPaymentSuccessEventThroughFacade() {
        // Given
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                "payment-event-789",
                "PAYMENT_SUCCESS",
                "v1",
                System.currentTimeMillis(),
                "{\"orderId\":12345,\"productId\":1,\"quantity\":2,\"totalPrice\":2000}"
        );

        RankingScore rankingScore = RankingScore.forPaymentSuccess(
                1L, java.math.BigDecimal.valueOf(2000), System.currentTimeMillis()
        );
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("order-events", 0, 0, null, envelope);

        when(eventProcessingFacade.processOrderEvent(envelope))
                .thenReturn(OrderEventResult.processed(rankingScore));

        // When
        consumer.onOrderEvents(List.of(record), acknowledgment);

        // Then
        verify(eventProcessingFacade, times(1)).processOrderEvent(envelope);
        verify(eventProcessingFacade, times(1)).updateRankingScores(anyList(), isNull());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("개별 메시지 처리 실패가 전체 배치를 실패시키지 않아야 한다")
    void shouldContinueProcessingWhenIndividualMessageFails() {
        // Given
        DomainEventEnvelope validEnvelope = new DomainEventEnvelope(
                "valid-event",
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                "{\"productId\":1,\"userId\":100}"
        );

        DomainEventEnvelope invalidEnvelope = new DomainEventEnvelope(
                "invalid-event",
                "PRODUCT_VIEW",
                "v1",
                System.currentTimeMillis(),
                "invalid-payload"
        );

        RankingScore rankingScore = RankingScore.forProductView(1L, System.currentTimeMillis());

        ConsumerRecord<Object, Object> validRecord = new ConsumerRecord<>("catalog-events", 0, 0, null, validEnvelope);
        ConsumerRecord<Object, Object> invalidRecord = new ConsumerRecord<>("catalog-events", 0, 1, null, invalidEnvelope);

        when(eventProcessingFacade.processCatalogEvent(validEnvelope))
                .thenReturn(CatalogEventResult.processed(rankingScore));
        when(eventProcessingFacade.processCatalogEvent(invalidEnvelope))
                .thenThrow(new RuntimeException("Processing failed"));

        // When
        consumer.onCatalogEvents(List.of(validRecord, invalidRecord), acknowledgment);

        // Then - 유효한 메시지는 처리되고, 전체 배치는 ack 되어야 함
        verify(eventProcessingFacade, times(1)).updateRankingScores(
                argThat(list -> list.size() == 1),
                isNull()
        );
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("여러 이벤트의 랭킹 점수가 배치로 업데이트되어야 한다")
    void shouldBatchUpdateRankingScores() {
        // Given
        DomainEventEnvelope envelope1 = new DomainEventEnvelope(
                "event-1", "PRODUCT_VIEW", "v1", System.currentTimeMillis(), "{\"productId\":1}"
        );
        DomainEventEnvelope envelope2 = new DomainEventEnvelope(
                "event-2", "LIKE_ACTION", "v1", System.currentTimeMillis(), "{\"productId\":2,\"action\":\"LIKE\"}"
        );

        RankingScore score1 = RankingScore.forProductView(1L, System.currentTimeMillis());
        RankingScore score2 = RankingScore.forLikeAction(2L, System.currentTimeMillis());

        ConsumerRecord<Object, Object> record1 = new ConsumerRecord<>("catalog-events", 0, 0, null, envelope1);
        ConsumerRecord<Object, Object> record2 = new ConsumerRecord<>("catalog-events", 0, 1, null, envelope2);

        when(eventProcessingFacade.processCatalogEvent(envelope1))
                .thenReturn(CatalogEventResult.processed(score1));
        when(eventProcessingFacade.processCatalogEvent(envelope2))
                .thenReturn(CatalogEventResult.processed(score2));

        // When
        consumer.onCatalogEvents(List.of(record1, record2), acknowledgment);

        // Then - 2개의 랭킹 점수가 배치로 업데이트되어야 함
        verify(eventProcessingFacade, times(1)).updateRankingScores(argThat(list -> list.size() == 2), isNull());
        verify(acknowledgment, times(1)).acknowledge();
    }
}
