package com.loopers.domain.ranking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingEventLogRepository {
    
    Optional<RankingEventLog> findByEventId(String eventId);
    
    List<RankingEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Object[]> aggregateByProductIdAndTimeRange(LocalDateTime start, LocalDateTime end);
    
    List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt);
    
    RankingEventLog save(RankingEventLog eventLog);
}

