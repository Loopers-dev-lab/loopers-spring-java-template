package com.loopers.application.inbox;

import com.loopers.domain.inbox.EventInbox;
import com.loopers.domain.inbox.EventInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EventInbox Service
 * - Consumer의 멱등성 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventInboxService {

    private final EventInboxRepository eventInboxRepository;

    /**
     * 중복 이벤트 체크
     *
     * @param eventId 이벤트 ID
     * @return 중복 여부
     */
    public boolean isDuplicate(String eventId) {
        boolean exists = eventInboxRepository.existsByEventId(eventId);

        if (exists) {
            log.info("중복 이벤트 감지 - eventId: {}", eventId);
        }

        return exists;
    }

    /**
     * Inbox에 이벤트 저장 (처리 완료 마킹)
     *
     * @param eventId 이벤트 ID
     * @param aggregateType Aggregate Type
     * @param aggregateId Aggregate ID
     * @param eventType Event Type
     */
    @Transactional
    public void save(String eventId, String aggregateType, String aggregateId, String eventType) {
        EventInbox inbox = EventInbox.builder()
            .eventId(eventId)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .build();

        eventInboxRepository.save(inbox);

        log.info("Inbox에 이벤트 저장 - eventId: {}, eventType: {}, aggregateId: {}",
            eventId, eventType, aggregateId);
    }
}
