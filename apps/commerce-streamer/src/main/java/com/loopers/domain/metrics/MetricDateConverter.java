package com.loopers.domain.metrics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class MetricDateConverter {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private MetricDateConverter() {}

  public static Integer toMetricDate(Long epochMillis, ZoneId zoneId) {
    if (epochMillis == null) {
      throw new IllegalArgumentException("epochMillis는 null일 수 없습니다");
    }
    String dateStr = Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDate()
        .format(DATE_FORMATTER);
    return Integer.parseInt(dateStr);
  }
}
