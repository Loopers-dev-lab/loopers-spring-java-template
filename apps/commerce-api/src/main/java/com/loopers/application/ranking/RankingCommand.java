package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingPeriod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record RankingCommand(
        LocalDate date,
        RankingPeriod period,
        int page,
        int size
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static RankingCommand of(String date, String period, int page, int size) {
        LocalDate parsedDate = (date == null || date.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(date, DATE_FORMATTER);

        RankingPeriod rankingPeriod = parsePeriod(period);

        return new RankingCommand(parsedDate, rankingPeriod, page, size);
    }

    public static RankingCommand of(String date, int page, int size) {
        return of(date, "daily", page, size);
    }

    private static RankingPeriod parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return RankingPeriod.DAILY;
        }
        try {
            return RankingPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RankingPeriod.DAILY;
        }
    }
}
