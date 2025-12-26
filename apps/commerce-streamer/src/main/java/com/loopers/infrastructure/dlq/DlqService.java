package com.loopers.infrastructure.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Dead Letter Queue (DLQ) 처리 서비스
 * 
 * 재시도 횟수를 초과한 메시지나 영구적으로 처리 불가능한 메시지를 DLQ 토픽으로 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqService {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 실패한 메시지를 DLQ 토픽으로 전송
     * 
     * @param originalTopic 원본 토픽 이름
     * @param key 메시지 키 (partition key)
     * @param message 원본 메시지
     * @param errorMessage 에러 메시지
     * @param retryCount 재시도 횟수
     */
    public void sendToDlq(String originalTopic, Object key, Object message, String errorMessage, int retryCount) {
        try {
            String dlqTopic = determineDlqTopic(originalTopic);
            
            // DLQ 메시지 구조 생성
            DlqMessage dlqMessage = DlqMessage.builder()
                    .originalTopic(originalTopic)
                    .originalKey(key != null ? key.toString() : null)
                    .originalMessage(message)
                    .errorMessage(errorMessage)
                    .retryCount(retryCount)
                    .failedAt(java.time.Instant.now().toString())
                    .build();

            // DLQ 토픽으로 전송
            kafkaTemplate.send(dlqTopic, key, dlqMessage);
            
            log.error("Message sent to DLQ: originalTopic={}, dlqTopic={}, key={}, retryCount={}, error={}",
                    originalTopic, dlqTopic, key, retryCount, errorMessage);
        } catch (Exception e) {
            log.error("Failed to send message to DLQ: originalTopic={}, key={}", originalTopic, key, e);
            // DLQ 전송 실패는 치명적이므로 예외를 다시 던짐
            throw new RuntimeException("Failed to send message to DLQ", e);
        }
    }

    /**
     * 원본 토픽 이름으로부터 DLQ 토픽 이름 결정
     * 
     * @param originalTopic 원본 토픽 이름
     * @return DLQ 토픽 이름 (예: catalog-events -> catalog-events.dlq)
     */
    private String determineDlqTopic(String originalTopic) {
        return originalTopic + ".dlq";
    }
}

