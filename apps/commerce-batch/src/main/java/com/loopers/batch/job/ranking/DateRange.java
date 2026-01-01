package com.loopers.batch.job.ranking;

// 집계 기간 (시작일, 종료일 - YYYYMMDD 형식)
public record DateRange(int startDate, int endDate) {

  public static DateRange of(int startDate, int endDate) {
    return new DateRange(startDate, endDate);
  }
}