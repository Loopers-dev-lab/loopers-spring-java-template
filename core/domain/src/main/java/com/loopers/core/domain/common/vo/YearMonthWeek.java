package com.loopers.core.domain.common.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
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

    public LocalDate getWeekStartDate() {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate date = LocalDate.of(this.year, this.month, 1);

        return date.with(weekFields.weekOfYear(), this.weekOfYear)
                .with(ChronoField.DAY_OF_WEEK, 1);
    }

    public LocalDate getWeekEndDate() {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate date = LocalDate.of(this.year, this.month, 1);

        return date.with(weekFields.weekOfYear(), this.weekOfYear)
                .with(ChronoField.DAY_OF_WEEK, 7);
    }

    public LocalDateTime getWeekStartDateTime() {
        return this.getWeekStartDate().atStartOfDay();
    }

    public LocalDateTime getWeekEndDateTime() {
        return this.getWeekEndDate().atTime(23, 59, 59);
    }
}
