package com.loopers.core.domain.product.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record YearMonth(Integer year, Integer month) {

    public static YearMonth from(LocalDate date) {
        return new YearMonth(date.getYear(), date.getMonthValue());
    }

    public static YearMonth from(LocalDateTime dateTime) {
        return from(dateTime.toLocalDate());
    }
}
