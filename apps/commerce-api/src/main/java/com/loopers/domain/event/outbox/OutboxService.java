package com.loopers.domain.event.outbox;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.DomainEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 개별 이벤트 처리 (독립적인 트랜잭션)
     *
     * @param event 처리할 이벤트
     * @return 성공 여부
     */
    @Transactional
    public boolean processEvent(OutboxEventEntity event) {
        try {
            final DomainEventEnvelope envelope = new DomainEventEnvelope(
                    event.getEventId(),
                    event.getEventType(),
                    event.getVersion(),
                    event.getOccurredAtEpochMillis(),
                    event.getPayloadJson()
            );

            // Kafka 발송
            domainEventPublisher.publish(event.getTopic(), event.getMessageKey(), envelope);

            // 성공 시 상태 업데이트
            event.markSent();
            outboxRepository.save(event);

            log.debug("이벤트 발송 성공 - eventId: {}, topic: {}", event.getEventId(), event.getTopic());
            return true;

        } catch (Exception ex) {
            // 실패 시 상태 업데이트
            event.markFailed();
            outboxRepository.save(event);

            log.warn("이벤트 발송 실패 - eventId: {}, topic: {}, retryCount: {}, error: {}",
                    event.getEventId(), event.getTopic(), event.getRetryCount(), ex.getMessage());
            return false;
        }
    }

    public List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status) {
        return outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(status);
    }

    /**
     * 지정된 개수만큼 READY 상태 이벤트 조회 (배치 크기 조절 가능)
     */
    public List<OutboxEventEntity> findTopNByStatusOrderByCreatedAtAsc(OutboxStatus status, int limit) {
        return outboxRepository.findTopNByStatusOrderByCreatedAtAsc(status, limit);
    }

    public List<OutboxEventEntity> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(OutboxStatus outboxStatus,
                                                                                             int maxRetryCount) {
        return outboxRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                outboxStatus, maxRetryCount);
    }

    /**
     * 지정된 개수만큼 재시도 가능한 실패 이벤트 조회 (배치 크기 조절 가능)
     */
    public List<OutboxEventEntity> findTopNByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus status, int maxRetryCount, int limit) {
        return outboxRepository.findTopNByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                status, maxRetryCount, limit);
    }
}
