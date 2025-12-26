package com.loopers.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.loopers.domain.dlq.DlqMessage;
import com.loopers.infrastructure.persistence.DlqMessageRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DLQ Publisher 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DlqPublisherTest {

    @Autowired
    private DlqPublisher dlqPublisher;

    @Autowired
    private DlqMessageRepository dlqMessageRepository;

    @Test
    @DisplayName("역직렬화 에러는 DLQ로 전송해야 함")
    void shouldSendJsonProcessingExceptionToDlq() {
        // given
        JsonProcessingException exception = new JsonProcessingException("Invalid JSON") {};

        // when
        boolean shouldSendToDlq = dlqPublisher.shouldSendToDlq(exception);

        // then
        assertThat(shouldSendToDlq).isTrue();
    }

    @Test
    @DisplayName("IllegalArgumentException은 DLQ로 전송해야 함")
    void shouldSendIllegalArgumentExceptionToDlq() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // when
        boolean shouldSendToDlq = dlqPublisher.shouldSendToDlq(exception);

        // then
        assertThat(shouldSendToDlq).isTrue();
    }

    @Test
    @DisplayName("RuntimeException은 재시도해야 함 (DLQ 전송 안 함)")
    void shouldRetryRuntimeException() {
        // given
        RuntimeException exception = new RuntimeException("Temporary error");

        // when
        boolean shouldSendToDlq = dlqPublisher.shouldSendToDlq(exception);

        // then
        assertThat(shouldSendToDlq).isFalse();
    }

    @Test
    @DisplayName("실패한 메시지를 DLQ 테이블에 저장")
    void shouldSaveFailedMessageToDlq() {
        // given
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "loopers.commerce.product-liked-v1",
            0,
            100L,
            "product-123",
            "{\"invalid\":\"json}"
        );
        JsonProcessingException exception = new JsonProcessingException("Parse error") {};

        // when
        dlqPublisher.publishToDlq(record, exception);

        // then
        DlqMessage savedMessage = dlqMessageRepository.findAll().get(0);
        assertThat(savedMessage.getOriginalTopic()).isEqualTo("loopers.commerce.product-liked-v1");
        assertThat(savedMessage.getOriginalPartition()).isEqualTo(0);
        assertThat(savedMessage.getOriginalOffset()).isEqualTo(100L);
        assertThat(savedMessage.getMessageKey()).isEqualTo("product-123");
        assertThat(savedMessage.getMessageValue()).isEqualTo("{\"invalid\":\"json}");
        assertThat(savedMessage.getErrorType()).isEqualTo("JsonProcessingException");
        assertThat(savedMessage.isResolved()).isFalse();
    }

    @Test
    @DisplayName("DLQ 메시지는 스택 트레이스를 포함해야 함")
    void shouldIncludeStackTrace() {
        // given
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "test-topic",
            0,
            1L,
            "key",
            "value"
        );
        Exception exception = new RuntimeException("Test error");

        // when
        dlqPublisher.publishToDlq(record, exception);

        // then
        DlqMessage savedMessage = dlqMessageRepository.findAll().get(0);
        assertThat(savedMessage.getStackTrace()).isNotNull();
        assertThat(savedMessage.getStackTrace()).contains("RuntimeException");
        assertThat(savedMessage.getStackTrace()).contains("Test error");
    }
}
