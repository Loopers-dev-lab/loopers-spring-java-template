package com.loopers.domain.event;

import com.loopers.infrastructure.event.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dead Letter Queue 서비스
 * 처리 실패한 이벤트를 DB에 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final DeadLetterEventRepository deadLetterEventRepository;

    /**
     * 실패한 이벤트를 Dead Letter Queue에 저장합니다.
     *
     * @param topic Kafka 토픽
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param eventId 이벤트 ID
     * @param payload 페이로드 (JSON 문자열)
     * @param errorMessage 에러 메시지
     * @param retryCount 재시도 횟수
     */
    @Transactional
    public void saveFailedEvent(String topic, Integer partition, Long offset, 
                               String eventId, String payload, String errorMessage, int retryCount) {
        try {
            DeadLetterEvent deadLetterEvent = DeadLetterEvent.builder()
                    .topic(topic)
                    .partition(partition)
                    .offset(offset)
                    .eventId(eventId != null ? eventId : "unknown")
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .retryCount(retryCount)
                    .build();

            deadLetterEventRepository.save(deadLetterEvent);
            log.warn("Failed event saved to DLQ - topic: {}, partition: {}, offset: {}, eventId: {}, retryCount: {}",
                    topic, partition, offset, eventId, retryCount);
        } catch (Exception e) {
            log.error("Failed to save event to DLQ - topic: {}, partition: {}, offset: {}",
                    topic, partition, offset, e);
            // DLQ 저장 실패는 로그만 남기고 예외를 던지지 않음 (원본 메시지 처리 흐름 방해 방지)
        }
    }
}

