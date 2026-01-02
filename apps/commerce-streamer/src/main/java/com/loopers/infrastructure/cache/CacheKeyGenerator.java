package com.loopers.infrastructure.cache;

/**
 * Redis 캐시 키 생성기 (commerce-streamer)
 * commerce-api의 CacheKeyGenerator와 동일한 키 형식을 사용
 */
public class CacheKeyGenerator {

    private static final String VERSION = "v1";

    /**
     * Daily ranking ZSET key
     * 형식: ranking:all:v1:{yyyyMMdd}
     * 예: ranking:all:v1:20251224
     */
    public static String dailyRankingKey(String date) {
        return String.format("ranking:all:%s:%s", VERSION, date);
    }
}
