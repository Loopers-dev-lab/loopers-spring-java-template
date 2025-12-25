package com.loopers.domain.eventhandled;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EventHandledService {
    private final EventHandledRepository eventHandledRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyHandled(String eventId) {
        return eventHandledRepository.existsByEventId(eventId);
    }

    /**
     * 이벤트 처리 완료 기록
     */
    @Transactional
    public void markAsHandled(String eventId, String eventType,
                              String aggregateType, String aggregateId) {
        EventHandled eventHandled = EventHandled.create(
                eventId, eventType, aggregateType, aggregateId
        );
        eventHandledRepository.save(eventHandled);
    }
}
