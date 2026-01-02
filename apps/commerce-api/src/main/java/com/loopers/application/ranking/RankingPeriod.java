package com.loopers.application.ranking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Locale;

public enum RankingPeriod {
    DAILY("daily") {
        @Override
        public LocalDate resolveStartDate(String date) {
            return parse(date);
        }

        @Override
        public String resolveKey(String date) {
            return parse(date).format(FORMATTER);
        }
    },
    WEEKLY("weekly") {
        @Override
        public LocalDate resolveStartDate(String date) {
            LocalDate target = parse(date);
            return target.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        @Override
        public String resolveKey(String date) {
            return toYearMonthWeek(resolveStartDate(date));
        }
    },
    MONTHLY("monthly") {
        @Override
        public LocalDate resolveStartDate(String date) {
            LocalDate target = parse(date);
            return target.withDayOfMonth(1);
        }

        @Override
        public String resolveKey(String date) {
            return toYearMonth(resolveStartDate(date));
        }
    };

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final String value;

    RankingPeriod(String value) {
        this.value = value;
    }

    public static RankingPeriod from(String value) {
        if (value == null) {
            return DAILY;
        }
        return Arrays.stream(values())
                .filter(period -> period.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(DAILY);
    }

    public abstract LocalDate resolveStartDate(String date);

    public abstract String resolveKey(String date);

    public boolean isDaily() {
        return this == DAILY;
    }

    private static LocalDate parse(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now(ZONE_ID);
        }
        return LocalDate.parse(date, FORMATTER);
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
