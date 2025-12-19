package com.loopers.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.OutboxEvent;
import com.loopers.domain.event.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalOutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionalOutboxEventPublisher eventPublisher;

    @Test
    @DisplayName("이벤트를 Outbox 테이블에 저장한다")
    void publish_savesToOutbox() throws Exception {
        // given
        String topic = "test-topic";
        String key = "test-key";
        String event = "test-event";
        when(objectMapper.writeValueAsString(event)).thenReturn("\"test-event\"");

        // when
        eventPublisher.publish(topic, key, event);

        // then
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}

