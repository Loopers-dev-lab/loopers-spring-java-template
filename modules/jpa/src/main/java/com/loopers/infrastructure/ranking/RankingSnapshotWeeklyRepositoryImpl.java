package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotWeekly;
import com.loopers.domain.ranking.RankingSnapshotWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingSnapshotWeeklyRepositoryImpl implements RankingSnapshotWeeklyRepository {

    private final RankingSnapshotWeeklyJpaRepository rankingSnapshotWeeklyJpaRepository;

    @Override
    public Optional<RankingSnapshotWeekly> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime) {
        return rankingSnapshotWeeklyJpaRepository.findByProductIdAndSnapshotTime(productId, snapshotTime);
    }

    @Override
    public List<RankingSnapshotWeekly> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime) {
        return rankingSnapshotWeeklyJpaRepository.findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
    }

    @Override
    public List<RankingSnapshotWeekly> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end) {
        return rankingSnapshotWeeklyJpaRepository.findBySnapshotTimeBetween(start, end);
    }

    @Override
    public Optional<RankingSnapshotWeekly> findTopByOrderBySnapshotTimeDesc() {
        return rankingSnapshotWeeklyJpaRepository.findTopByOrderBySnapshotTimeDesc();
    }

    @Override
    public List<RankingSnapshotWeekly> findLatestSnapshotOrderByProductRank() {
        return rankingSnapshotWeeklyJpaRepository.findLatestSnapshotOrderByProductRank();
    }

    @Override
    public List<RankingSnapshotWeekly> findBySnapshotTimeOrderByProductRank(LocalDateTime snapshotTime) {
        return rankingSnapshotWeeklyJpaRepository.findBySnapshotTimeOrderByProductRank(snapshotTime);
    }

    @Override
    public RankingSnapshotWeekly save(RankingSnapshotWeekly snapshot) {
        return rankingSnapshotWeeklyJpaRepository.save(snapshot);
    }

    @Override
    public void delete(RankingSnapshotWeekly snapshot) {
        rankingSnapshotWeeklyJpaRepository.delete(snapshot);
    }

    @Override
    public long count() {
        return rankingSnapshotWeeklyJpaRepository.count();
    }
}

