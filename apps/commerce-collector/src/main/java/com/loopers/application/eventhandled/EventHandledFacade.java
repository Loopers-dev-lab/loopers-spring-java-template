package com.loopers.application.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventHandledFacade {
    private final EventHandledRepository eventHandledRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyHandled(String eventId) {
        return eventHandledRepository.existsByEventId(eventId);
    }

    /**
     * 여러 이벤트 중 이미 처리된 이벤트 ID 조회 (N+1 방지)
     * @param eventIds 확인할 이벤트 ID 목록
     * @return 이미 처리된 이벤트 ID Set
     */
    @Transactional(readOnly = true)
    public Set<String> findAlreadyHandledEventIds(List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }

        return eventHandledRepository.findAllByEventIdIn(eventIds)
                .stream()
                .map(EventHandled::getEventId)
                .collect(Collectors.toSet());
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

    /**
     * 이벤트 처리 완료 기록 (배치)
     * - 대량 이벤트 처리 시 성능 향상
     */
    @Transactional
    public void markAsHandledBatch(List<EventHandledInfo> eventInfos) {
        if (eventInfos == null || eventInfos.isEmpty()) {
            return;
        }

        List<EventHandled> eventHandledList = eventInfos.stream()
                .map(info -> EventHandled.create(
                        info.eventId(),
                        info.eventType(),
                        info.aggregateType(),
                        info.aggregateId()
                ))
                .collect(Collectors.toList());

        eventHandledRepository.saveAll(eventHandledList);
    }
}
