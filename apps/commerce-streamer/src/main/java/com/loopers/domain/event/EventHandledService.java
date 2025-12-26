package com.loopers.domain.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트 처리 완료 Domain Service
 * <p>
 * 이벤트 멱등성 처리를 담당하는 Domain 계층 서비스입니다.
 * 이벤트 ID 기반으로 중복 처리를 방지합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 26.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventHandledService {

    private final EventRepository eventRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyHandled(String eventId) {
        return eventRepository.existsById(eventId);
    }

    /**
     * 이벤트 처리 완료 마킹
     * 
     * @return true: 저장 성공 (처음 처리), false: 저장 실패 (이미 처리됨 또는 동시성 충돌)
     */
    @Transactional
    public boolean markAsHandled(String eventId) {
        try {
            // 트랜잭션 내에서 다시 한번 확인 (동시성 안전)
            if (eventRepository.existsById(eventId)) {
                log.debug("트랜잭션 내 중복 확인: {}", eventId);
                return false;
            }

            eventRepository.save(EventEntity.create(eventId));
            log.debug("이벤트 처리 완료 저장: {}", eventId);
            return true;
        } catch (Exception e) {
            // 동시성으로 인한 중복 저장 시도 (Unique 제약 조건 위반 등)
            log.debug("동시성으로 인한 이벤트 저장 실패: {}", eventId, e);
            return false;
        }
    }
}
