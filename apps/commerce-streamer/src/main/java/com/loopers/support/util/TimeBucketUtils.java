package com.loopers.support.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
public class TimeBucketUtils {

  private static final DateTimeFormatter BUCKET_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  /**
   * 현재 시간 기준 10분 단위 버킷 시간 생성
   *
   * @return 10분 단위로 버킷팅된 LocalDateTime
   */
  public static LocalDateTime getCurrentBucketTime() {
    LocalDateTime now = LocalDateTime.now();
    int bucketMinutes = (now.getMinute() / 10) * 10;
    return now.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
  }

  /**
   * 이벤트 시간 기준 10분 단위 버킷 시간 생성
   *
   * @param eventTime ZonedDateTime 이벤트 시간
   * @return 10분 단위로 버킷팅된 LocalDateTime, eventTime이 null이면 null 반환
   */
  public static LocalDateTime getBucketTime(ZonedDateTime eventTime) {
    try {
      if (eventTime == null) {
        return null;
      }
      LocalDateTime eventDateTime = eventTime.toLocalDateTime();
      int bucketMinutes = (eventDateTime.getMinute() / 10) * 10;
      return eventDateTime.truncatedTo(ChronoUnit.HOURS).plusMinutes(bucketMinutes);
    } catch (Exception e) {
      log.warn("Failed to parse eventTime: eventTime={}", eventTime, e);
      return null;
    }
  }

  /**
   * LocalDateTime을 yyyyMMddHHmm 문자열로 변환
   *
   * @param bucketTime 버킷 시간
   * @return yyyyMMddHHmm 형식의 문자열, bucketTime이 null이면 null 반환
   */
  public static String toBucketTimeKey(LocalDateTime bucketTime) {
    if (bucketTime == null) {
      return null;
    }
    return bucketTime.format(BUCKET_TIME_FORMATTER);
  }

  /**
   * yyyyMMddHHmm 문자열을 LocalDateTime으로 변환
   *
   * @param bucketTimeKey yyyyMMddHHmm 형태의 문자열
   * @return LocalDateTime
   */
  public static LocalDateTime parseBucketTimeKey(String bucketTimeKey) {
    if (bucketTimeKey == null || bucketTimeKey.length() != 12) {
      throw new IllegalArgumentException("bucketTimeKey must be in yyyyMMddHHmm format");
    }

    try {
      String year = bucketTimeKey.substring(0, 4);
      String month = bucketTimeKey.substring(4, 6);
      String day = bucketTimeKey.substring(6, 8);
      String hour = bucketTimeKey.substring(8, 10);
      String minute = bucketTimeKey.substring(10, 12);

      return LocalDateTime.of(
          Integer.parseInt(year),
          Integer.parseInt(month),
          Integer.parseInt(day),
          Integer.parseInt(hour),
          Integer.parseInt(minute)
      );
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid bucketTimeKey format: " + bucketTimeKey, e);
    }
  }
}