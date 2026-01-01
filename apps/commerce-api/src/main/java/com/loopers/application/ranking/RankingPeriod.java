package com.loopers.application.ranking;

import lombok.Getter;

@Getter
public enum RankingPeriod {
    DAILY("daily", "ranking:all:"),
    WEEKLY("weekly", "ranking:weekly:"),
    MONTHLY("monthly", "ranking:monthly:");

    private final String value;
    private final String redisKeyPrefix;

    RankingPeriod(String value, String redisKeyPrefix) {
        this.value = value;
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public static RankingPeriod fromString(String period) {
        if (period == null || period.trim().isEmpty()) {
            return DAILY; // 기본값
        }
        
        for (RankingPeriod p : RankingPeriod.values()) {
            if (p.value.equalsIgnoreCase(period.trim())) {
                return p;
            }
        }
        
        throw new IllegalArgumentException("지원하지 않는 기간입니다: " + period + ". 지원 기간: daily, weekly, monthly");
    }
}