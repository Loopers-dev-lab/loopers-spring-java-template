package com.loopers.domain.ranking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public record RankingPeriod(
        LocalDate startDate,
        LocalDate endDate
) {
    public static RankingPeriod ofWeek(LocalDate date) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new RankingPeriod(weekStart, weekEnd);
    }

    public static RankingPeriod ofMonth(LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        return new RankingPeriod(monthStart, monthEnd);
    }
}

