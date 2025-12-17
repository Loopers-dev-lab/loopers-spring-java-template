package com.loopers.domain.inbox;

/**
 * EventInbox Repository
 */
public interface EventInboxRepository {

    /**
     * eventId로 중복 체크
     */
    boolean existsByEventId(String eventId);

    /**
     * EventInbox 저장
     */
    EventInbox save(EventInbox eventInbox);

    /**
     * 모든 EventInbox 삭제 (테스트용)
     */
    void deleteAll();
}
