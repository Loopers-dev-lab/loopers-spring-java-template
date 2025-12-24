package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.auditlog.AuditLogCommand;
import com.loopers.application.auditlog.AuditLogFacade;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer {

    private final AuditLogFacade auditLogFacade;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.user-action-name}",
            groupId = "${kafka.consumer.audit-log-group}",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void listen(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment acknowledgment
    ) {
        try {
            for (ConsumerRecord<String, String> record : records) {
                String payload = record.value();
                String userId = record.key();

                String eventType = "";
                if (record.headers().lastHeader("eventType") != null) {
                    eventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
                }

                if (userId == null || userId.isBlank()) {
                    log.warn("userId is blank, payload = {}", payload);
                    continue;
                }

                try {
                    AuditLogCommand command = parsePayload(payload, eventType);
                    auditLogFacade.processAuditLog(command);
                } catch (Exception e) {
                    log.error("AuditLog 처리 실패: payload={}", payload, e);
                }
            }
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private AuditLogCommand parsePayload(String payload, String eventType) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return new AuditLogCommand(
                    node.has("eventId") ? node.get("eventId").asText() : null,
                    node.has("userId") ? node.get("userId").asLong() : null,
                    node.has("actionType") ? node.get("actionType").asText() : eventType,
                    node.has("targetType") ? node.get("targetType").asText() : null,
                    node.has("targetId") ? node.get("targetId").asLong() : null,
                    payload
            );
        } catch (Exception e) {
            log.error("Payload 파싱 실패: {}", payload, e);
            return new AuditLogCommand(null, null, eventType, null, null, payload);
        }
    }
}
