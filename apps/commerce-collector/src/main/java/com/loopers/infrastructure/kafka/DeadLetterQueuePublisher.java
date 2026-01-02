package com.loopers.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead Letter Queue (DLQ) 메시지 발행 담당
 * 처리 실패한 Kafka 메시지를 DLQ 토픽으로 전송하여 재처리 가능하도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueuePublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 원본 메시지를 DLQ로 전송 (파싱 실패 케이스)
     *
     * @param dlqTopic DLQ 토픽명
     * @param originalMessage 원본 메시지 (JSON 문자열)
     * @param key 메시지 키
     * @param offset Kafka offset
     * @param error 발생한 예외
     */
    public void sendRawMessageToDLQ(
            String dlqTopic,
            String originalMessage,
            String key,
            Long offset,
            Exception error
    ) {
        try {
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("originalMessage", originalMessage);
            dlqMessage.put("errorMessage", error.getMessage());
            dlqMessage.put("errorType", error.getClass().getName());
            dlqMessage.put("stackTrace", getStackTraceAsString(error));
            dlqMessage.put("offset", offset);
            dlqMessage.put("failedAt", ZonedDateTime.now().toString());
            dlqMessage.put("retryable", isRetryable(error));

            String dlqPayload = objectMapper.writeValueAsString(dlqMessage);
            kafkaTemplate.send(dlqTopic, key, dlqPayload);

            log.warn("DLQ 전송 완료 [원본 메시지] - topic: {}, offset: {}, error: {}",
                    dlqTopic, offset, error.getMessage());

        } catch (Exception e) {
            log.error("DLQ 전송 실패 - topic: {}, offset: {}", dlqTopic, offset, e);
            // DLQ 전송도 실패하면 로그만 남기고 계속 진행 (메시지 손실 방지)
        }
    }

    /**
     * 파싱된 이벤트를 DLQ로 전송 (처리 실패 케이스)
     *
     * @param dlqTopic DLQ 토픽명
     * @param event 파싱된 이벤트 객체
     * @param eventId 이벤트 ID
     * @param error 발생한 예외
     */
    public void sendParsedEventToDLQ(
            String dlqTopic,
            Object event,
            String eventId,
            Exception error
    ) {
        try {
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("event", event);
            dlqMessage.put("eventId", eventId);
            dlqMessage.put("errorMessage", error.getMessage());
            dlqMessage.put("errorType", error.getClass().getName());
            dlqMessage.put("stackTrace", getStackTraceAsString(error));
            dlqMessage.put("failedAt", ZonedDateTime.now().toString());
            dlqMessage.put("retryable", isRetryable(error));

            String dlqPayload = objectMapper.writeValueAsString(dlqMessage);
            kafkaTemplate.send(dlqTopic, eventId, dlqPayload);

            log.warn("DLQ 전송 완료 [파싱된 이벤트] - topic: {}, eventId: {}, error: {}",
                    dlqTopic, eventId, error.getMessage());

        } catch (Exception e) {
            log.error("DLQ 전송 실패 - topic: {}, eventId: {}", dlqTopic, eventId, e);
        }
    }

    /**
     * 배치 처리 실패 시 전체 배치를 DLQ로 전송
     *
     * @param dlqTopic DLQ 토픽명
     * @param events 실패한 이벤트 리스트
     * @param error 발생한 예외
     */
    public void sendBatchToDLQ(
            String dlqTopic,
            Iterable<?> events,
            Exception error
    ) {
        int successCount = 0;
        int failCount = 0;

        for (Object event : events) {
            try {
                String eventId = extractEventId(event);
                sendParsedEventToDLQ(dlqTopic, event, eventId, error);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("배치 DLQ 전송 실패 - event: {}", event, e);
            }
        }

        log.warn("배치 DLQ 전송 완료 - topic: {}, 성공: {}, 실패: {}",
                dlqTopic, successCount, failCount);
    }

    /**
     * 예외가 재시도 가능한지 판단
     */
    private boolean isRetryable(Exception error) {
        // JsonProcessingException, IllegalArgumentException 등은 재시도 불가
        String errorType = error.getClass().getName();
        return !errorType.contains("JsonProcessingException")
                && !errorType.contains("IllegalArgumentException")
                && !errorType.contains("NullPointerException");
    }

    /**
     * 스택 트레이스를 문자열로 변환 (상위 10줄만)
     */
    private String getStackTraceAsString(Exception error) {
        StackTraceElement[] stackTrace = error.getStackTrace();
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(10, stackTrace.length);
        for (int i = 0; i < limit; i++) {
            sb.append(stackTrace[i].toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 이벤트 객체에서 eventId 추출 (리플렉션 사용)
     */
    private String extractEventId(Object event) {
        try {
            var method = event.getClass().getMethod("eventId");
            Object result = method.invoke(event);
            return result != null ? result.toString() : "unknown";
        } catch (Exception e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }
}
