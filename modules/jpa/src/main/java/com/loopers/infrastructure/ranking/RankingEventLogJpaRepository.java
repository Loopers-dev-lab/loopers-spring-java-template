package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingEventLogJpaRepository extends JpaRepository<RankingEventLog, Long> {
    
    Optional<RankingEventLog> findByEventId(String eventId);
    
    List<RankingEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT r.productId, SUM(r.score) as totalScore " +
           "FROM RankingEventLog r " +
           "WHERE r.occurredAt BETWEEN :start AND :end " +
           "GROUP BY r.productId")
    List<Object[]> aggregateByProductIdAndTimeRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT r.productId, r.eventType, COUNT(r) as eventCount, " +
           "COALESCE(SUM(r.rawPrice), 0) as sumRawPrice, " +
           "COALESCE(SUM(r.rawQuantity), 0) as sumRawQuantity, " +
           "COALESCE(SUM(CASE WHEN r.eventType = 'ORDER' AND r.rawPrice IS NOT NULL AND r.rawQuantity IS NOT NULL " +
           "THEN LOG10(r.rawPrice * r.rawQuantity + 1) ELSE 0 END), 0) as sumOrderScore " +
           "FROM RankingEventLog r " +
           "WHERE r.occurredAt BETWEEN :start AND :end " +
           "GROUP BY r.productId, r.eventType")
    List<Object[]> aggregateByProductIdAndEventTypeAndTimeRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt);
}

