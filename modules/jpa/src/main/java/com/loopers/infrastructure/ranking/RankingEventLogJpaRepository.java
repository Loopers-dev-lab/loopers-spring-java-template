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
    
    List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt);
}

