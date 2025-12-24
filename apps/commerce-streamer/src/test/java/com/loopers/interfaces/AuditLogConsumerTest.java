package com.loopers.interfaces;

import com.loopers.application.auditlog.AuditLogCommand;
import com.loopers.application.auditlog.AuditLogFacade;
import com.loopers.interfaces.consumer.AuditLogConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditLogConsumerTest {

    private final AuditLogFacade facade = mock(AuditLogFacade.class);
    private final AuditLogConsumer consumer = new AuditLogConsumer(facade, new com.fasterxml.jackson.databind.ObjectMapper());

    private ConsumerRecord<String, String> makeRecord(
            String topic, String key, String value, String eventTypeHeader
    ) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>(topic, 0, 0L, key, value);
        if (eventTypeHeader != null) {
            rec.headers().add("eventType", eventTypeHeader.getBytes(StandardCharsets.UTF_8));
        }
        return rec;
    }

    @Test
    @DisplayName("userAction 토픽 → JSON 파싱 → Facade.processAuditLog 호출")
    void listen_withMapPayload() {
        // given
        String topic = "user-action-events";
        String userId = "oyy";

        String jsonValue = """
                {
                    "eventId": "evt-audit-123",
                    "traceId": "sdwers3rdgsdf",
                    "userId": 100,
                    "actionType": "PAYMENT_PROCESS",
                    "targetType": "ORDER",
                    "targetId": 4,
                    "payload": {
                        "orderId": 4,
                        "paymentId": 2,
                        "totalPrice": 40000,
                        "paymentMethod": "POINT"
                    },
                    "occurredAt": "2025-09-04T12:34:56"
                }
                """;

        ConsumerRecord<String, String> record = makeRecord(topic, userId, jsonValue, "PAYMENT_PROCESS");

        // when
        consumer.listen(List.of(record), mock(org.springframework.kafka.support.Acknowledgment.class));

        // then
        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(facade, times(1)).processAuditLog(captor.capture());

        AuditLogCommand captured = captor.getValue();
        assertThat(captured.eventId()).isEqualTo("evt-audit-123");
        assertThat(captured.userId()).isEqualTo(100L);
        assertThat(captured.actionType()).isEqualTo("PAYMENT_PROCESS");
    }

    @Test
    @DisplayName("userId가 blank면 처리하지 않음")
    void listen_blankUserId_skip() {
        // given
        String jsonValue = "{\"eventId\": \"evt-123\"}";
        ConsumerRecord<String, String> record = makeRecord("user-action-events", "", jsonValue, null);

        // when
        consumer.listen(List.of(record), mock(org.springframework.kafka.support.Acknowledgment.class));

        // then
        verify(facade, never()).processAuditLog(any());
    }
}
