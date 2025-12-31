package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.MetricsAggregator;
import com.loopers.application.ranking.RankingAggregator;
import com.loopers.application.EventHandledService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CatalogEventConsumerTest {

    @InjectMocks
    private CatalogEventConsumer consumer;

    @Mock
    private MetricsAggregator metricsAggregator;

    @Mock
    private EventHandledService eventHandledService;

    @Mock
    private RankingAggregator rankingAggregator;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new CatalogEventConsumer(
                objectMapper,
                eventHandledService,
                metricsAggregator,
                rankingAggregator
        );
    }

    @Test
    void LIKE_CREATED_이벤트면_like_증가() throws Exception {
        when(eventHandledService.tryMarkHandled("e1", "LIKE_CREATED"))
                .thenReturn(true);

        Map<String, Object> event = Map.of(
                "id", "e1",
                "aggregateType", "LIKE",
                "aggregateId", "1",
                "eventType", "LIKE_CREATED",
                "occurredAt", "2025-12-19T12:00:00",
                "payload", Map.of(
                        "userId", "u1",
                        "productId", 1L
                )
        );
        String message = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("catalog-events", 0, 0L, null, message);

        consumer.consume(List.of(record));

        verify(metricsAggregator, times(1)).aggregate(org.mockito.ArgumentMatchers.anyList());
        verify(rankingAggregator, times(1)).aggregate(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void LIKE_DELETED_이벤트면_like_감소() throws Exception {
        when(eventHandledService.tryMarkHandled("e2", "LIKE_DELETED"))
                .thenReturn(true);

        Map<String, Object> event = Map.of(
                "id", "e2",
                "aggregateType", "LIKE",
                "aggregateId", "1",
                "eventType", "LIKE_DELETED",
                "occurredAt", "2025-12-19T12:00:00",
                "payload", Map.of(
                        "userId", "u1",
                        "productId", 1L
                )
        );
        String message = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("catalog-events", 0, 0L, null, message);

        consumer.consume(List.of(record));

        verify(metricsAggregator, times(1)).aggregate(org.mockito.ArgumentMatchers.anyList());
        verify(rankingAggregator, times(1)).aggregate(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void 동일한_eventId_중복_전달시_첫_1회만_처리() throws Exception {
        when(eventHandledService.tryMarkHandled("dup-1", "LIKE_CREATED"))
                .thenReturn(true)  // first
                .thenReturn(false) // second
                .thenReturn(false);// third

        Map<String, Object> event = Map.of(
                "id", "dup-1",
                "aggregateType", "LIKE",
                "aggregateId", "1",
                "eventType", "LIKE_CREATED",
                "occurredAt", "2025-12-19T12:00:00",
                "payload", Map.of(
                        "userId", "u1",
                        "productId", 1L
                )
        );
        String message = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("catalog-events", 0, 0L, null, message);

        consumer.consume(List.of(record, record, record));

        verify(metricsAggregator, times(1)).aggregate(org.mockito.ArgumentMatchers.anyList());
        verifyNoMoreInteractions(metricsAggregator);
        verify(rankingAggregator, times(1)).aggregate(org.mockito.ArgumentMatchers.anyList());
        verifyNoMoreInteractions(rankingAggregator);
    }
}


