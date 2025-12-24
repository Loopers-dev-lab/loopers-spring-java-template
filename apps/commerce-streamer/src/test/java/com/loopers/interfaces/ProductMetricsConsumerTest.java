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
}
