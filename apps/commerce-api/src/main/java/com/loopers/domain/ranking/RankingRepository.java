package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface RankingRepository {

    // Weekly
    List<WeeklyRanking> findWeeklyByDateOrderByRank(LocalDate weekStart, LocalDate weekEnd, int limit, int offset);

    Optional<WeeklyRanking> findWeeklyByProductIdAndDate(Long productId, LocalDate weekStart, LocalDate weekEnd);

    long countWeeklyByDate(LocalDate weekStart, LocalDate weekEnd);

    // Monthly
    List<MonthlyRanking> findMonthlyByPeriodOrderByRank(YearMonth period, int limit, int offset);

    Optional<MonthlyRanking> findMonthlyByProductIdAndPeriod(Long productId, YearMonth period);

    long countMonthlyByPeriod(YearMonth period);
}
