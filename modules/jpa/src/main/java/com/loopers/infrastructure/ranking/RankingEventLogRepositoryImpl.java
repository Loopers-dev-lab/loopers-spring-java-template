package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class RankingEventLogRepositoryImpl implements RankingEventLogRepository {

    private final RankingEventLogJpaRepository rankingEventLogJpaRepository;

    @Override
    public Optional<RankingEventLog> findByEventId(String eventId) {
        return rankingEventLogJpaRepository.findByEventId(eventId);
    }

    @Override
    public Set<String> findAllEventIdsByEventIdIn(Collection<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Set.of();
        }
        return rankingEventLogJpaRepository.findAllEventIdsByEventIdIn(eventIds);
    }

    @Override
    public List<RankingEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end) {
        return rankingEventLogJpaRepository.findByOccurredAtBetween(start, end);
    }

    @Override
    public List<Object[]> aggregateByProductIdAndTimeRange(LocalDateTime start, LocalDateTime end) {
        return rankingEventLogJpaRepository.aggregateByProductIdAndTimeRange(start, end);
    }

    @Override
    public List<Object[]> aggregateByProductIdAndEventTypeAndTimeRange(LocalDateTime start, LocalDateTime end) {
        return rankingEventLogJpaRepository.aggregateByProductIdAndEventTypeAndTimeRange(start, end);
    }

    @Override
    public List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt) {
        return rankingEventLogJpaRepository.findByOccurredAtAfter(occurredAt);
    }

    @Override
    public RankingEventLog save(RankingEventLog eventLog) {
        return rankingEventLogJpaRepository.save(eventLog);
    }

    @Override
    public List<RankingEventLog> saveAll(List<RankingEventLog> eventLogs) {
        if (eventLogs == null || eventLogs.isEmpty()) {
            return List.of();
        }
        return rankingEventLogJpaRepository.saveAll(eventLogs);
    }
}

