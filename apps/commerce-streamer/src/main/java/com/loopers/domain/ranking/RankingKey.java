package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Redis ZSET 랭킹 키 생성 전략
 */
public class RankingKey {

    private static final String DAILY_PREFIX = "ranking:all:";
    private static final String HOURLY_PREFIX = "ranking:realtime:";
    private static final DateTimeFormatter DAILY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOURLY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    /**
     * 일간 랭킹 키 생성
     * @param date 대상 날짜
     * @return ranking:all:yyyyMMdd
     */
    public static String daily(LocalDate date) {
        return DAILY_PREFIX + date.format(DAILY_FORMATTER);
    }

    /**
     * 오늘 일간 랭킹 키 생성
     */
    public static String dailyToday() {
        return daily(LocalDate.now());
    }

    /**
     * 어제 일간 랭킹 키 생성
     */
    public static String dailyYesterday() {
        return daily(LocalDate.now().minusDays(1));
    }

    /**
     * 내일 일간 랭킹 키 생성 (콜드 스타트용)
     */
    public static String dailyTomorrow() {
        return daily(LocalDate.now().plusDays(1));
    }

    /**
     * 시간 단위 실시간 랭킹 키 생성
     * @param dateTime 대상 시간
     * @return ranking:realtime:yyyyMMddHH
     */
    public static String hourly(LocalDateTime dateTime) {
        return HOURLY_PREFIX + dateTime.format(HOURLY_FORMATTER);
    }

    /**
     * 현재 시간의 실시간 랭킹 키 생성
     */
    public static String hourlyNow() {
        return hourly(LocalDateTime.now());
    }

    /**
     * 문자열 날짜로 일간 랭킹 키 생성
     * @param dateString yyyyMMdd 형식
     */
    public static String daily(String dateString) {
        LocalDate date = LocalDate.parse(dateString, DAILY_FORMATTER);
        return daily(date);
    }
}
