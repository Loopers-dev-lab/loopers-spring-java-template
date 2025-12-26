package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotYearly;
import com.loopers.domain.ranking.RankingSnapshotYearlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingSnapshotYearlyRepositoryImpl implements RankingSnapshotYearlyRepository {

    private final RankingSnapshotYearlyJpaRepository rankingSnapshotYearlyJpaRepository;

    @Override
    public Optional<RankingSnapshotYearly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime) {
        return rankingSnapshotYearlyJpaRepository.findByProductIdAndSnapshotTime(productId, snapshotTime);
    }

    @Override
    public List<RankingSnapshotYearly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime) {
        return rankingSnapshotYearlyJpaRepository.findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
    }

    @Override
    public List<RankingSnapshotYearly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end) {
        return rankingSnapshotYearlyJpaRepository.findBySnapshotTimeBetween(start, end);
    }

    @Override
    public Optional<RankingSnapshotYearly> findTopByOrderBySnapshotTimeDesc() {
        return rankingSnapshotYearlyJpaRepository.findTopByOrderBySnapshotTimeDesc();
    }

    @Override
    public RankingSnapshotYearly save(RankingSnapshotYearly snapshot) {
        return rankingSnapshotYearlyJpaRepository.save(snapshot);
    }
}

