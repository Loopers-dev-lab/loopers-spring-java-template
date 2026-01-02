package com.loopers.application.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.infrastructure.ranking.MvProductRankMonthlyJpaRepository;
import com.loopers.infrastructure.ranking.MvProductRankWeeklyJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingMaterializedViewService {

    private final MvProductRankWeeklyJpaRepository weeklyRepository;
    private final MvProductRankMonthlyJpaRepository monthlyRepository;

    public List<ProductRankSnapshot> getRankings(RankingPeriod period, String periodKey, int page, int size) {
        List<ProductRankSnapshot> snapshots = fetch(period, periodKey);
        int safeSize = Math.max(size, 1);
        long skip = (long) Math.max(page, 0) * safeSize;
        return snapshots.stream()
                .skip(skip)
                .limit(safeSize)
                .toList();
    }

    public long count(RankingPeriod period, String periodKey) {
        return fetch(period, periodKey).size();
    }

    private List<ProductRankSnapshot> fetch(RankingPeriod period, String periodKey) {
        if (period == RankingPeriod.WEEKLY) {
            return weeklyRepository.findByIdPeriodKeyOrderByRankAsc(periodKey).stream()
                    .map(this::fromWeekly)
                    .toList();
        }
        if (period == RankingPeriod.MONTHLY) {
            return monthlyRepository.findByIdPeriodKeyOrderByRankAsc(periodKey).stream()
                    .map(this::fromMonthly)
                    .toList();
        }
        throw new IllegalArgumentException("Unsupported period for MV: " + period);
    }

    private ProductRankSnapshot fromWeekly(MvProductRankWeekly entity) {
        return new ProductRankSnapshot(entity.getRank(), entity.getProductId(), entity.getScore());
    }

    private ProductRankSnapshot fromMonthly(MvProductRankMonthly entity) {
        return new ProductRankSnapshot(entity.getRank(), entity.getProductId(), entity.getScore());
    }
}
