package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingEventLogRepository {
    
    Optional<RankingEventLog> findByEventId(String eventId);
    
    List<RankingEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Object[]> aggregateByProductIdAndTimeRange(LocalDateTime start, LocalDateTime end);
    
    /**
     * 이벤트 타입별로 그룹화하여 집계
     * 반환값: [productId, eventType, count, sumRawPrice, sumRawQuantity]
     */
    List<Object[]> aggregateByProductIdAndEventTypeAndTimeRange(LocalDateTime start, LocalDateTime end);
    
    List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt);
    
    RankingEventLog save(RankingEventLog eventLog);
}

