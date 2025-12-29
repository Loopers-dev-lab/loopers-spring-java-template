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
}
