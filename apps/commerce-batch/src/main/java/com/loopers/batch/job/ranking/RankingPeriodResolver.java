package com.loopers.batch.job.ranking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class RankingPeriodResolver {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private RankingPeriodResolver() {
    }

    public static RankingPeriod weekly(String targetDate) {
        LocalDate target = parse(targetDate);
        LocalDate weekStart = target.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new RankingPeriod(toYearMonthWeek(weekStart));
    }

    public static RankingPeriod monthly(String targetDate) {
        LocalDate target = parse(targetDate);
        LocalDate monthStart = target.withDayOfMonth(1);
        return new RankingPeriod(toYearMonth(monthStart));
    }

    private static LocalDate parse(String targetDate) {
        if (!StringUtils.hasText(targetDate)) {
            return LocalDate.now(ZONE_ID);
        }
        return LocalDate.parse(targetDate, FORMATTER);
    }

    private static String toYearMonthWeek(LocalDate target) {
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int weekBasedYear = target.get(weekFields.weekBasedYear());
        int week = target.get(weekFields.weekOfWeekBasedYear());
        return String.format("%04d-W%02d", weekBasedYear, week);
    }

    private static String toYearMonth(LocalDate target) {
        return String.format("%04d-%02d", target.getYear(), target.getMonthValue());
    }
}
