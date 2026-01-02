package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotMonthly;
import com.loopers.domain.ranking.RankingSnapshotMonthlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingSnapshotMonthlyRepositoryImpl implements RankingSnapshotMonthlyRepository {

    private final RankingSnapshotMonthlyJpaRepository rankingSnapshotMonthlyJpaRepository;

    @Override
    public Optional<RankingSnapshotMonthly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime) {
        return rankingSnapshotMonthlyJpaRepository.findByProductIdAndSnapshotTime(productId, snapshotTime);
    }

    @Override
    public List<RankingSnapshotMonthly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime) {
        return rankingSnapshotMonthlyJpaRepository.findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
    }

    @Override
    public List<RankingSnapshotMonthly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end) {
        return rankingSnapshotMonthlyJpaRepository.findBySnapshotTimeBetween(start, end);
    }

    @Override
    public Optional<RankingSnapshotMonthly> findTopByOrderBySnapshotTimeDesc() {
        return rankingSnapshotMonthlyJpaRepository.findTopByOrderBySnapshotTimeDesc();
    }

    @Override
    public List<RankingSnapshotMonthly> findLatestSnapshotOrderByRank() {
        return rankingSnapshotMonthlyJpaRepository.findLatestSnapshotOrderByRank();
    }

    @Override
    public RankingSnapshotMonthly save(RankingSnapshotMonthly snapshot) {
        return rankingSnapshotMonthlyJpaRepository.save(snapshot);
    }

    @Override
    public void delete(RankingSnapshotMonthly snapshot) {
        rankingSnapshotMonthlyJpaRepository.delete(snapshot);
    }

    @Override
    public long count() {
        return rankingSnapshotMonthlyJpaRepository.count();
    }
}

