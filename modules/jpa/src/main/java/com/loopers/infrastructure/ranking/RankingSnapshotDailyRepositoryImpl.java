package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RankingSnapshotDailyRepositoryImpl implements RankingSnapshotDailyRepository {

    private final RankingSnapshotDailyJpaRepository rankingSnapshotDailyJpaRepository;

    @Override
    public Optional<RankingSnapshotDaily> findByProductIdAndSnapshotTime(Long productId, LocalDateTime snapshotTime) {
        return rankingSnapshotDailyJpaRepository.findByProductIdAndSnapshotTime(productId, snapshotTime);
    }

    @Override
    public List<RankingSnapshotDaily> findBySnapshotTimeOrderByTotalScoreDesc(LocalDateTime snapshotTime) {
        return rankingSnapshotDailyJpaRepository.findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);
    }

    @Override
    public List<RankingSnapshotDaily> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end) {
        return rankingSnapshotDailyJpaRepository.findBySnapshotTimeBetween(start, end);
    }

    @Override
    public Optional<RankingSnapshotDaily> findTopByOrderBySnapshotTimeDesc() {
        return rankingSnapshotDailyJpaRepository.findTopByOrderBySnapshotTimeDesc();
    }

    @Override
    public RankingSnapshotDaily save(RankingSnapshotDaily snapshot) {
        return rankingSnapshotDailyJpaRepository.save(snapshot);
    }
}

