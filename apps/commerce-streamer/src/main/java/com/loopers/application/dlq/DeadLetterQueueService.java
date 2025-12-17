package com.loopers.application.dlq;

import com.loopers.domain.dlq.DeadLetterQueue;
import com.loopers.domain.dlq.DeadLetterQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DeadLetterQueue Service
 * - 처리 실패한 메시지 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final DeadLetterQueueRepository deadLetterQueueRepository;

    /**
     * DLQ에 실패 메시지 저장
     *
     * @param originalTopic 원본 Topic
     * @param partitionKey Partition Key
     * @param eventId 이벤트 ID
     * @param payload 원본 메시지
     * @param errorMessage 에러 메시지
     * @param retryCount 재시도 횟수
     */
    @Transactional
    public void save(String originalTopic, String partitionKey, String eventId,
                     String payload, String errorMessage, int retryCount) {

        DeadLetterQueue dlq = DeadLetterQueue.builder()
            .originalTopic(originalTopic)
            .partitionKey(partitionKey)
            .eventId(eventId)
            .payload(payload)
            .errorMessage(errorMessage)
            .retryCount(retryCount)
            .build();

        deadLetterQueueRepository.save(dlq);

        log.error("⚠️ DLQ에 메시지 저장 - topic: {}, eventId: {}, retryCount: {}, error: {}",
            originalTopic, eventId, retryCount, errorMessage);
    }
}
