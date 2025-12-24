package com.loopers.interfaces;

import com.loopers.domain.dlq.DlqMessageService;
import com.loopers.interfaces.consumer.DlqConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DlqConsumerTest {

    private final DlqMessageService dlqMessageService = mock(DlqMessageService.class);
    private final DlqConsumer consumer = new DlqConsumer(dlqMessageService);

    private ConsumerRecord<String, String> makeRecord(
            String topic, int partition, long offset, String key, String value
    ) {
        return new ConsumerRecord<>(topic, partition, offset, key, value);
    }

    @Nested
    @DisplayName("DLQ 메시지 소비")
    class ConsumeDlqMessage {

        @Test
        @DisplayName("DLQ 메시지 → DlqMessageService.saveDlqMessage 호출")
        void consume_callsSaveDlqMessage() {
            // given
            String dlqTopic = "product-like-metrics.DLT";
            String key = "123";
            String value = "{\"eventId\":\"evt-001\",\"productId\":123}";
            int partition = 0;
            long offset = 100L;

            ConsumerRecord<String, String> record = makeRecord(dlqTopic, partition, offset, key, value);
            Acknowledgment ack = mock(Acknowledgment.class);

            // when
            consumer.consume(List.of(record), ack);

            // then
            verify(dlqMessageService, times(1)).saveDlqMessage(
                    eq("product-like-metrics"),  // originalTopic (.DLT 제거)
                    eq(partition),
                    eq(offset),
                    eq(key),
                    eq(value),
                    anyString()
            );
            verify(ack, times(1)).acknowledge();
        }

        @Test
        @DisplayName(".DLT 접미사가 제거된 원본 토픽명이 저장된다")
        void consume_extractsOriginalTopic() {
            // given
            String dlqTopic = "order-events.DLT";
            ConsumerRecord<String, String> record = makeRecord(dlqTopic, 0, 0L, "order-456", "{}");
            Acknowledgment ack = mock(Acknowledgment.class);

            // when
            consumer.consume(List.of(record), ack);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(dlqMessageService).saveDlqMessage(
                    topicCaptor.capture(),
                    anyInt(),
                    anyLong(),
                    anyString(),
                    anyString(),
                    anyString()
            );

            assertThat(topicCaptor.getValue()).isEqualTo("order-events");
        }

        @Test
        @DisplayName("여러 DLQ 메시지 배치 처리")
        void consume_batchMessages() {
            // given
            List<ConsumerRecord<String, String>> records = List.of(
                    makeRecord("product-like-metrics.DLT", 0, 1L, "key-1", "{\"id\":1}"),
                    makeRecord("product-stock-metrics.DLT", 0, 2L, "key-2", "{\"id\":2}"),
                    makeRecord("order-events.DLT", 0, 3L, "key-3", "{\"id\":3}")
            );
            Acknowledgment ack = mock(Acknowledgment.class);

            // when
            consumer.consume(records, ack);

            // then
            verify(dlqMessageService, times(3)).saveDlqMessage(
                    anyString(), anyInt(), anyLong(), anyString(), anyString(), anyString()
            );
            verify(ack, times(1)).acknowledge();
        }

        @Test
        @DisplayName("partition, offset, key, value가 정확히 전달된다")
        void consume_passesCorrectParameters() {
            // given
            String dlqTopic = "user-action-events.DLT";
            int partition = 2;
            long offset = 999L;
            String key = "user-123";
            String value = "{\"eventId\":\"evt-fail\",\"userId\":123}";

            ConsumerRecord<String, String> record = makeRecord(dlqTopic, partition, offset, key, value);
            Acknowledgment ack = mock(Acknowledgment.class);

            // when
            consumer.consume(List.of(record), ack);

            // then
            ArgumentCaptor<Integer> partitionCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Long> offsetCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

            verify(dlqMessageService).saveDlqMessage(
                    eq("user-action-events"),
                    partitionCaptor.capture(),
                    offsetCaptor.capture(),
                    keyCaptor.capture(),
                    valueCaptor.capture(),
                    anyString()
            );

            assertThat(partitionCaptor.getValue()).isEqualTo(partition);
            assertThat(offsetCaptor.getValue()).isEqualTo(offset);
            assertThat(keyCaptor.getValue()).isEqualTo(key);
            assertThat(valueCaptor.getValue()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("저장 실패해도 다음 메시지 처리 및 acknowledge 호출")
        void consume_continuesOnException() {
            // given
            List<ConsumerRecord<String, String>> records = List.of(
                    makeRecord("topic-1.DLT", 0, 1L, "key-1", "{}"),
                    makeRecord("topic-2.DLT", 0, 2L, "key-2", "{}"),
                    makeRecord("topic-3.DLT", 0, 3L, "key-3", "{}")
            );
            Acknowledgment ack = mock(Acknowledgment.class);

            // 두 번째 호출에서 예외 발생
            doNothing()
                    .doThrow(new RuntimeException("DB 저장 실패"))
                    .doNothing()
                    .when(dlqMessageService).saveDlqMessage(
                            anyString(), anyInt(), anyLong(), anyString(), anyString(), anyString()
                    );

            // when
            consumer.consume(records, ack);

            // then - 3번 모두 호출 시도, acknowledge도 호출
            verify(dlqMessageService, times(3)).saveDlqMessage(
                    anyString(), anyInt(), anyLong(), anyString(), anyString(), anyString()
            );
            verify(ack, times(1)).acknowledge();
        }

        @Test
        @DisplayName("빈 레코드 리스트도 정상 처리")
        void consume_emptyRecords() {
            // given
            Acknowledgment ack = mock(Acknowledgment.class);

            // when
            consumer.consume(List.of(), ack);

            // then
            verify(dlqMessageService, never()).saveDlqMessage(
                    anyString(), anyInt(), anyLong(), anyString(), anyString(), anyString()
            );
            verify(ack, times(1)).acknowledge();
        }
    }

    @Nested
    @DisplayName("토픽명 추출")
    class TopicExtraction {

        @Test
        @DisplayName("다양한 DLT 토픽 패턴에서 원본 토픽 추출")
        void consume_variousTopicPatterns() {
            // given
            List<ConsumerRecord<String, String>> records = List.of(
                    makeRecord("simple.DLT", 0, 1L, "k1", "{}"),
                    makeRecord("multi-word-topic.DLT", 0, 2L, "k2", "{}"),
                    makeRecord("namespace.topic.name.DLT", 0, 3L, "k3", "{}")
            );
            Acknowledgment ack = mock(Acknowledgment.class);

            // when
            consumer.consume(records, ack);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(dlqMessageService, times(3)).saveDlqMessage(
                    topicCaptor.capture(),
                    anyInt(), anyLong(), anyString(), anyString(), anyString()
            );

            List<String> capturedTopics = topicCaptor.getAllValues();
            assertThat(capturedTopics).containsExactly(
                    "simple",
                    "multi-word-topic",
                    "namespace.topic.name"
            );
        }
    }
}
