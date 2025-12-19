package com.loopers.domain.event;

import com.loopers.infrastructure.event.BaseInboxEventRepository;
import com.loopers.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox 패턴을 통한 이벤트 멱등성 처리를 위한 서비스
 * 
 * 각 도메인별 InboxEventRepository를 사용하여 이미 처리된 이벤트인지 확인하고,
 * 처리되지 않은 경우 InboxEvent를 저장합니다.
 * 
 * 이 서비스는 DB 트랜잭션 내에서 호출되어야 하며,
 * 비즈니스 로직과 동일한 트랜잭션에서 실행되어야 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxEventService {

    /**
     * 이벤트가 이미 처리되었는지 확인하고, 처리되지 않은 경우 InboxEvent를 저장합니다.
     * 
     * @param repository 도메인별 InboxEventRepository
     * @param event 처리할 이벤트
     * @param topic Kafka topic
     * @param inboxEventBuilder InboxEvent 빌더 (도메인별 InboxEvent 생성용)
     * @param <T> InboxEvent 타입
     * @return 이미 처리된 이벤트면 true, 새로 처리하는 이벤트면 false
     */
    @Transactional
    public <T extends BaseInboxEvent> boolean checkAndSave(
            BaseInboxEventRepository<T> repository,
            DomainEvent event,
            String topic,
            InboxEventBuilder<T> inboxEventBuilder
    ) {
        String eventId = event.getEventId();
        
        if (eventId == null || eventId.isBlank()) {
            log.warn("EventId is null or blank, allowing processing");
            return false;
        }

        // 이미 처리된 이벤트인지 확인
        if (repository.existsByEventId(eventId)) {
            log.info("Duplicate event detected in Inbox - eventId: {}, type: {}, topic: {}", 
                    eventId, event.getClass().getSimpleName(), topic);
            return true;
        }

        // 새 이벤트로 InboxEvent 저장
        String aggregateId = extractAggregateId(event);
        String type = event.getClass().getSimpleName();
        
        T inboxEvent = inboxEventBuilder.build(eventId, aggregateId, type, topic);
        repository.save(inboxEvent);
        
        log.debug("Saved InboxEvent - eventId: {}, type: {}, topic: {}", 
                eventId, type, topic);
        return false;
    }

    /**
     * 이벤트에서 aggregateId를 추출합니다.
     * DomainEvent의 getPartitionKey()를 사용합니다.
     */
    private String extractAggregateId(DomainEvent event) {
        // partitionKey를 사용 (일반적으로 aggregateId와 동일)
        String partitionKey = event.getPartitionKey();
        if (partitionKey != null && !partitionKey.isBlank()) {
            return partitionKey;
        }
        
        // partitionKey가 없으면 eventId를 fallback으로 사용
        log.warn("Could not extract aggregateId from event, using eventId as fallback - eventId: {}", 
                event.getEventId());
        return event.getEventId();
    }

    /**
     * InboxEvent를 생성하는 함수형 인터페이스
     */
    @FunctionalInterface
    public interface InboxEventBuilder<T extends BaseInboxEvent> {
        T build(String eventId, String aggregateId, String type, String topic);
    }
}

