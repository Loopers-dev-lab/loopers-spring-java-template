package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RankingEventLogRepository {
    
    Optional<RankingEventLog> findByEventId(String eventId);
    
    /**
     * 여러 eventId에 대해 이미 처리된 eventId 목록을 조회 (멱등성 체크용)
     */
    Set<String> findAllEventIdsByEventIdIn(Collection<String> eventIds);
    
    List<RankingEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Object[]> aggregateByProductIdAndTimeRange(LocalDateTime start, LocalDateTime end);
    
    /**
     * 이벤트 타입별로 그룹화하여 집계
     * 반환값: [productId, eventType, count, sumRawPrice, sumRawQuantity]
     */
    List<Object[]> aggregateByProductIdAndEventTypeAndTimeRange(LocalDateTime start, LocalDateTime end);
    
    List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt);
    
    RankingEventLog save(RankingEventLog eventLog);
    
    /**
     * 여러 이벤트 로그를 한 번에 저장 (배치 저장)
     */
    List<RankingEventLog> saveAll(List<RankingEventLog> eventLogs);
}

