package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingEventLogRepositoryImpl implements RankingEventLogRepository {

    private final RankingEventLogJpaRepository rankingEventLogJpaRepository;

    @Override
    public Optional<RankingEventLog> findByEventId(String eventId) {
        return rankingEventLogJpaRepository.findByEventId(eventId);
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
    public List<RankingEventLog> findByOccurredAtAfter(LocalDateTime occurredAt) {
        return rankingEventLogJpaRepository.findByOccurredAtAfter(occurredAt);
    }

    @Override
    public RankingEventLog save(RankingEventLog eventLog) {
        return rankingEventLogJpaRepository.save(eventLog);
    }
}

