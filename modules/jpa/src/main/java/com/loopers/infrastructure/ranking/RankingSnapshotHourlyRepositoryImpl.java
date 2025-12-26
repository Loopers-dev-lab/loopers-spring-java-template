package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotHourlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingSnapshotHourlyRepositoryImpl implements RankingSnapshotHourlyRepository {

    private final RankingSnapshotHourlyJpaRepository rankingSnapshotHourlyJpaRepository;

    @Override
    public Optional<RankingSnapshotHourly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime) {
        return rankingSnapshotHourlyJpaRepository.findByProductIdAndSnapshotTime(productId, snapshotTime);
    }

    @Override
    public List<RankingSnapshotHourly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime) {
        return rankingSnapshotHourlyJpaRepository.findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
    }

    @Override
    public List<RankingSnapshotHourly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end) {
        return rankingSnapshotHourlyJpaRepository.findBySnapshotTimeBetween(start, end);
    }

    @Override
    public Optional<RankingSnapshotHourly> findTopByOrderBySnapshotTimeDesc() {
        return rankingSnapshotHourlyJpaRepository.findTopByOrderBySnapshotTimeDesc();
    }

    @Override
    public RankingSnapshotHourly save(RankingSnapshotHourly snapshot) {
        return rankingSnapshotHourlyJpaRepository.save(snapshot);
    }

    @Override
    public long count() {
        return rankingSnapshotHourlyJpaRepository.count();
    }
}

