package com.loopers.application.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record RankingCommand(
        LocalDate date,
        int page,
        int size
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    public RankingCommand {
        // 유효성 검증
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (date == null) {
            date = LocalDate.now();
        }
    }

    public static RankingCommand of(String dateString, int page, int size) {
        LocalDate date = parseDate(dateString);
        return new RankingCommand(date, page, size);
    }

    public static RankingCommand today(int page, int size) {
        return new RankingCommand(LocalDate.now(), page, size);
    }

    private static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }
}

