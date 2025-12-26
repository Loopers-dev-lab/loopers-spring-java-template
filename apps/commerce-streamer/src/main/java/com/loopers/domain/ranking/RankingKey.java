package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RankingKey {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private RankingKey() {
    }

    /**
     * 일간 랭킹 키 생성
     */
    public static String daily(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }

    /**
     * 오늘 랭킹 키 생성
     */
    public static String today() {
        return daily(LocalDate.now());
    }

    /**
     * 어제 랭킹 키 생성
     */
    public static String yesterday() {
        return daily(LocalDate.now().minusDays(1));
    }

    /**
     * 날짜 문자열(yyyyMMdd)로부터 키 생성
     */
    public static String fromDateString(String dateString) {
        return KEY_PREFIX + dateString;
    }

    /**
     * 키에서 날짜 추출
     */
    public static String extractDate(String key) {
        return key.replace(KEY_PREFIX, "");
    }
}
