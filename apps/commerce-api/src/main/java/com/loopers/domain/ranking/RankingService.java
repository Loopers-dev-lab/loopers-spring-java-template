package com.loopers.domain.ranking;

import com.loopers.infrastructure.cache.RankingCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class RankingService {

    private final RankingCacheService rankingCacheService;
    private final MvProductRankWeeklyRepository mvProductRankWeeklyRepository;
    private final MvProductRankMonthlyRepository mvProductRankMonthlyRepository;

    public List<Ranking> getDailyRanking(LocalDate date, int page, int size) {
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        List<RankingItem> rankingItems = rankingCacheService.getRankingRange(date, start, end);

        if (rankingItems.isEmpty()) {
            return new ArrayList<>();
        }

        return IntStream.range(0, rankingItems.size())
                .mapToObj(i -> {
                    RankingItem item = rankingItems.get(i);
                    long rank = start + 1 + i;
                    return new Ranking(item.productId(), rank, item.score());
                })
                .toList();
    }

    public List<Ranking> getWeeklyRanking(LocalDate date, int page, int size) {
        RankingPeriod period = RankingPeriod.ofWeek(date);
        List<MvProductRankWeekly> weeklyRanks = mvProductRankWeeklyRepository
                .findByPeriodOrderByRankingAsc(
                        period.startDate(),
                        period.endDate(),
                        page,
                        size
                );

        if (weeklyRanks.isEmpty()) {
            return new ArrayList<>();
        }

        return weeklyRanks.stream()
                .filter(rank -> rank.getRanking() != null)
                .map(rank -> new Ranking(
                        rank.getProductId(),
                        rank.getRanking().longValue(),
                        rank.getScore()
                ))
                .toList();
    }

    public List<Ranking> getMonthlyRanking(LocalDate date, int page, int size) {
        RankingPeriod period = RankingPeriod.ofMonth(date);
        List<MvProductRankMonthly> monthlyRanks = mvProductRankMonthlyRepository
                .findByPeriodOrderByRankingAsc(
                        period.startDate(),
                        period.endDate(),
                        page,
                        size
                );

        if (monthlyRanks.isEmpty()) {
            return new ArrayList<>();
        }

        return monthlyRanks.stream()
                .filter(rank -> rank.getRanking() != null)
                .map(rank -> new Ranking(
                        rank.getProductId(),
                        rank.getRanking().longValue(),
                        rank.getScore()
                ))
                .toList();
    }
}

