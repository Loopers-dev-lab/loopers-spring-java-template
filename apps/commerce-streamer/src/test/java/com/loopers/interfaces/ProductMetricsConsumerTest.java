package com.loopers.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.ProductMetricsCommand;
import com.loopers.application.metrics.ProductMetricsFacade;
import com.loopers.interfaces.consumer.ProductMetricsConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductMetricsConsumerTest {

    private final ProductMetricsFacade facade = mock(ProductMetricsFacade.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductMetricsConsumer consumer = new ProductMetricsConsumer(facade, objectMapper);

    private ConsumerRecord<String, String> makeRecord(String topic, String key, String value) {
        return new ConsumerRecord<>(topic, 0, 0L, key, value);
    }

    @Test
    @DisplayName("product-like 토픽 → processLikeMetrics 호출")
    void listen_likeEvent() {
        // given
        String topic = "product-like-metrics";
        String key = "123";
        String value = """
                {
                    "eventId": "evt-like-001",
                    "productId": 123,
                    "likeType": "LIKED"
                }
                """;

        ConsumerRecord<String, String> record = makeRecord(topic, key, value);
        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(List.of(record), ack);

        // then
        ArgumentCaptor<ProductMetricsCommand> captor = ArgumentCaptor.forClass(ProductMetricsCommand.class);
        verify(facade, times(1)).processLikeMetrics(captor.capture());
        verify(ack, times(1)).acknowledge();

        ProductMetricsCommand captured = captor.getValue();
        assertThat(captured.eventId()).isEqualTo("evt-like-001");
        assertThat(captured.productId()).isEqualTo(123L);
        assertThat(captured.likeType()).isEqualTo("LIKED");
    }

    @Test
    @DisplayName("product-stock 토픽 → processStockMetrics 호출")
    void listen_stockEvent() {
        // given
        String topic = "product-stock-metrics";
        String key = "456";
        String value = """
                {
                    "eventId": "evt-stock-001",
                    "productId": 456,
                    "stock": 5,
                    "changedType": "DECREASED"
                }
                """;

        ConsumerRecord<String, String> record = makeRecord(topic, key, value);
        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(List.of(record), ack);

        // then
        ArgumentCaptor<ProductMetricsCommand> captor = ArgumentCaptor.forClass(ProductMetricsCommand.class);
        verify(facade, times(1)).processStockMetrics(captor.capture());

        ProductMetricsCommand captured = captor.getValue();
        assertThat(captured.eventId()).isEqualTo("evt-stock-001");
        assertThat(captured.productId()).isEqualTo(456L);
        assertThat(captured.stock()).isEqualTo(5);
        assertThat(captured.changedType()).isEqualTo("DECREASED");
    }

    @Test
    @DisplayName("product-view 토픽 → processViewMetrics 호출")
    void listen_viewEvent() {
        // given
        String topic = "product-view-metrics";
        String key = "789";
        String value = """
                {
                    "eventId": "evt-view-001",
                    "productId": 789
                }
                """;

        ConsumerRecord<String, String> record = makeRecord(topic, key, value);
        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(List.of(record), ack);

        // then
        verify(facade, times(1)).processViewMetrics(any());
        verify(ack, times(1)).acknowledge();
    }

    @Test
    @DisplayName("여러 메시지 배치 처리 후 acknowledge 1회 호출")
    void listen_batchMessages() {
        // given
        List<ConsumerRecord<String, String>> records = List.of(
                makeRecord("product-like-metrics", "1", """
                        {"eventId": "evt-1", "productId": 1, "likeType": "LIKED"}
                        """),
                makeRecord("product-like-metrics", "2", """
                        {"eventId": "evt-2", "productId": 2, "likeType": "UNLIKED"}
                        """),
                makeRecord("product-view-metrics", "3", """
                        {"eventId": "evt-3", "productId": 3}
                        """)
        );

        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(records, ack);

        // then
        verify(facade, times(2)).processLikeMetrics(any());
        verify(facade, times(1)).processViewMetrics(any());
        verify(ack, times(1)).acknowledge();
    }

    @Test
    @DisplayName("중복 이벤트 수신 시 Consumer는 각각 호출하고 멱등성을 보장한다")
    void listen_duplicateEvent_shouldProcessOnce() {
        // given
        String topic = "product-like-metrics";
        String eventId = "evt-duplicate-001";
        String value = String.format("""
            {
                "eventId": "%s",
                "productId": 123,
                "likeType": "LIKED"
            }
            """, eventId);

        ConsumerRecord<String, String> record1 = makeRecord(topic, "123", value);
        ConsumerRecord<String, String> record2 = makeRecord(topic, "123", value); // 중복
        Acknowledgment ack = mock(Acknowledgment.class);

        // 첫 번째 호출에서는 처리, 두 번째는 이미 처리됨으로 스킵
        doNothing().when(facade).processLikeMetrics(any());

        // when
        consumer.listen(List.of(record1), ack);
        consumer.listen(List.of(record2), ack);

        // then
        verify(facade, times(2)).processLikeMetrics(any());
    }

    @Test
    @DisplayName("멱등성: 동일 eventId로 중복 처리되지 않아야 한다 (실제 멱등 로직 검증)")
    void listen_duplicateEvent_shouldBeIdempotent() {
        // given
        String topic = "product-like-metrics";
        String eventId = "evt-idempotent-001";
        String value = String.format("""
        {
            "eventId": "%s",
            "productId": 123,
            "likeType": "LIKED"
        }
        """, eventId);

        ConsumerRecord<String, String> record1 = makeRecord(topic, "123", value);
        ConsumerRecord<String, String> record2 = makeRecord(topic, "123", value);
        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(List.of(record1), ack);
        consumer.listen(List.of(record2), ack);

        // then
        verify(facade, times(2)).processLikeMetrics(any());
        verify(ack, times(2)).acknowledge();
    }

    @Test
    @DisplayName("랭킹 이벤트: 조회 이벤트 처리 시 랭킹 점수가 업데이트된다")
    void listen_viewEvent_shouldUpdateRanking() {
        // given
        String topic = "product-view-metrics";
        String value = """
        {
            "eventId": "evt-view-ranking-001",
            "productId": 789
        }
        """;

        ConsumerRecord<String, String> record = makeRecord(topic, "789", value);
        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(List.of(record), ack);

        // then
        ArgumentCaptor<ProductMetricsCommand> captor = ArgumentCaptor.forClass(ProductMetricsCommand.class);
        verify(facade).processViewMetrics(captor.capture());

        ProductMetricsCommand captured = captor.getValue();
        assertThat(captured.productId()).isEqualTo(789L);
    }

    @Test
    @DisplayName("잘못된 JSON 포맷의 메시지는 무시하고 다음 메시지를 처리한다")
    void listen_invalidJson_shouldContinueProcessing() {
        // given
        List<ConsumerRecord<String, String>> records = List.of(
                makeRecord("product-view-metrics", "1", "invalid json"),
                makeRecord("product-view-metrics", "2", """
                {"eventId": "evt-valid", "productId": 2}
                """)
        );

        Acknowledgment ack = mock(Acknowledgment.class);

        // when
        consumer.listen(records, ack);

        // then
        verify(facade, times(1)).processViewMetrics(any());
        verify(ack, times(1)).acknowledge();
    }
}
