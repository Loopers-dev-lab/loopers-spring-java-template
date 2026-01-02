package com.loopers.infrastructure.ranking;

import com.loopers.batch.domain.ranking.MonthlyRanking;
import com.loopers.batch.domain.ranking.MonthlyRankingRepository;
import com.loopers.batch.domain.ranking.WeeklyRanking;
import com.loopers.batch.domain.ranking.WeeklyRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RankingRepositoryImpl implements WeeklyRankingRepository, MonthlyRankingRepository {

    private final WeeklyRankingJpaRepository weeklyJpaRepository;
    private final MonthlyRankingJpaRepository monthlyJpaRepository;

    @Override
    public void deleteByWeekStartAndWeekEnd(LocalDate weekStart, LocalDate weekEnd) {
        weeklyJpaRepository.deleteByWeekStartAndWeekEnd(weekStart, weekEnd);
    }

    @Override
    public void saveAll(List<WeeklyRanking> rankings) {
        weeklyJpaRepository.saveAll(rankings);
    }

    @Override
    public void deleteByMonthPeriod(YearMonth monthPeriod) {
        monthlyJpaRepository.deleteByMonthPeriod(monthPeriod);
    }

    @Override
    public void saveAll(List<MonthlyRanking> rankings) {
        monthlyJpaRepository.saveAll(rankings);
    }
}
