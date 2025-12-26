package com.loopers.infrastructure.kafka;

import com.loopers.domain.dlq.DlqMessage;
import com.loopers.infrastructure.persistence.DlqMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DLQ Publisher
 * - Consumer에서 처리 실패한 메시지를 DLQ 테이블에 저장
 * - 역직렬화 실패, 비즈니스 로직 에러 등 복구 불가능한 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqPublisher {

    private final DlqMessageRepository dlqMessageRepository;

    /**
     * Consumer Record를 DLQ에 저장
     *
     * @param record 실패한 Kafka 메시지
     * @param exception 발생한 예외
     */
    @Transactional
    public void publishToDlq(ConsumerRecord<String, String> record, Exception exception) {
        try {
            DlqMessage dlqMessage = DlqMessage.create(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value(),
                exception
            );

            dlqMessageRepository.save(dlqMessage);

            log.error("[DLQ] Message saved to DLQ - topic: {}, partition: {}, offset: {}, key: {}, error: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                exception.getClass().getSimpleName());

        } catch (Exception e) {
            // DLQ 저장마저 실패한 경우 (심각한 상황)
            log.error("[DLQ] CRITICAL: Failed to save message to DLQ - topic: {}, partition: {}, offset: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                e);
            // 여기서는 예외를 던지지 않음 (무한 루프 방지)
        }
    }

    /**
     * 에러 타입에 따라 DLQ 전송 여부 결정
     *
     * @param exception 발생한 예외
     * @return true = DLQ로 전송, false = 재시도 필요
     */
    public boolean shouldSendToDlq(Exception exception) {
        // 역직렬화 에러 → DLQ (재시도 불가능)
        if (exception instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            return true;
        }

        // IllegalArgumentException → DLQ (데이터 오류)
        if (exception instanceof IllegalArgumentException) {
            return true;
        }

        // NullPointerException → DLQ (데이터 오류)
        if (exception instanceof NullPointerException) {
            return true;
        }

        // 그 외의 에러는 일시적 장애일 수 있으므로 재시도
        return false;
    }
}
