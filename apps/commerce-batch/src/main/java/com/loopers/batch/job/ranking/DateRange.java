package com.loopers.batch.job.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// 집계 기간 (시작일, 종료일 - YYYYMMDD 형식)
public record DateRange(int startDate, int endDate) {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  public DateRange {
    validateDateFormat(startDate, "시작일");
    validateDateFormat(endDate, "종료일");
    if (startDate > endDate) {
      throw new IllegalArgumentException("시작일은 종료일보다 클 수 없습니다");
    }
  }

  public static DateRange of(int startDate, int endDate) {
    return new DateRange(startDate, endDate);
  }

  private static void validateDateFormat(int date, String fieldName) {
    try {
      LocalDate.parse(String.valueOf(date), DATE_FORMAT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(fieldName + "이 유효하지 않은 날짜 형식입니다: " + date);
    }
  }
}