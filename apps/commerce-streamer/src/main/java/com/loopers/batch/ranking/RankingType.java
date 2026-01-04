package com.loopers.batch.ranking;

/**
 * 랭킹 타입 (배치용)
 */
public enum RankingType {
    HOURLY,   // 시간 단위 랭킹 (최근 1시간)
    DAILY,    // 일 단위 랭킹 (최근 24시간)
    WEEKLY,   // 주 단위 랭킹 (최근 7일)
    MONTHLY   // 월 단위 랭킹 (최근 30일)
}

