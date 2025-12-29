package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis ZSET 키 생성 유틸리티
 */
public class RankingKey {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private RankingKey() {
    }

    public static String daily(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}
