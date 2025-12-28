package com.loopers.core.domain.common.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

public record YearMonthWeek(
        Integer year, Integer month, Integer weekOfYear
) {

    public static YearMonthWeek from(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekOfYear = date.get(weekFields.weekOfYear());

        return new YearMonthWeek(
                date.getYear(),
                date.getMonthValue(),
                weekOfYear
        );
    }

    public static YearMonthWeek from(LocalDateTime dateTime) {
        return from(dateTime.toLocalDate());
    }
}
