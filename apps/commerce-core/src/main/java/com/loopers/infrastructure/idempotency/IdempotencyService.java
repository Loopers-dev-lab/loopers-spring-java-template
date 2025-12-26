package com.loopers.infrastructure.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final EventHandledRepository eventHandledRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인
     * @return true: 이미 처리됨, false: 처리되지 않음
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyHandled(String eventId) {
        return eventHandledRepository.existsByEventId(eventId);
    }

    /**
     * 이벤트 처리 완료를 기록
     * 주의: 비즈니스 로직과 같은 트랜잭션 내에서 호출되어야 함
     */
    @Transactional
    public void markAsHandled(String eventId, String eventType, String aggregateId, String handlerName) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.warn("Event already handled: eventId={}", eventId);
            return;
        }

        EventHandled eventHandled = EventHandled.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .handlerName(handlerName)
                .build();

        eventHandledRepository.save(eventHandled);
        log.debug("Event marked as handled: eventId={}", eventId);
    }
}

