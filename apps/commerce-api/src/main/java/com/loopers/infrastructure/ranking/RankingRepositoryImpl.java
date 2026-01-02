package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RankingRepositoryImpl implements RankingRepository {

    private final WeeklyRankingJpaRepository weeklyJpaRepository;
    private final MonthlyRankingJpaRepository monthlyJpaRepository;

    @Override
    public List<WeeklyRanking> findWeeklyByDateOrderByRank(LocalDate weekStart, LocalDate weekEnd, int limit, int offset) {
        return weeklyJpaRepository.findByDateWithPagination(weekStart, weekEnd, limit, offset);
    }

    @Override
    public Optional<WeeklyRanking> findWeeklyByProductIdAndDate(Long productId, LocalDate weekStart, LocalDate weekEnd) {
        return weeklyJpaRepository.findByProductIdAndWeekStartAndWeekEnd(productId, weekStart, weekEnd);
    }

    @Override
    public long countWeeklyByDate(LocalDate weekStart, LocalDate weekEnd) {
        return weeklyJpaRepository.countByWeekStartAndWeekEnd(weekStart, weekEnd);
    }

    @Override
    public List<MonthlyRanking> findMonthlyByPeriodOrderByRank(YearMonth period, int limit, int offset) {
        return monthlyJpaRepository.findByPeriodWithPagination(period.toString(), limit, offset);
    }

    @Override
    public Optional<MonthlyRanking> findMonthlyByProductIdAndPeriod(Long productId, YearMonth period) {
        return monthlyJpaRepository.findByProductIdAndMonthPeriod(productId, period);
    }

    @Override
    public long countMonthlyByPeriod(YearMonth period) {
        return monthlyJpaRepository.countByMonthPeriod(period);
    }
}
