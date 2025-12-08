package com.loopers.infrastructure.cache;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캐시 전략: Hot/Warm/Cold 차별화된 TTL과 갱신 방식
 * <p>
 * - Hot: 배치 갱신, TTL 60분 (인기순, 1페이지)
 * - Warm: Cache-Aside, TTL 10분 (2~3페이지)
 * - Cold: 캐시 미사용 (4페이지 이상)
 */
@Getter
@RequiredArgsConstructor
public enum CacheStrategy {

    /**
     * Hot: 배치 갱신, TTL 60분
     * 인기순 정렬, 1페이지 (가장 빈번 조회)
     */
    HOT(30, TimeUnit.MINUTES, true, CacheUpdateStrategy.BATCH_REFRESH),

    /**
     * Warm: Cache-Aside, TTL 10분
     * 2~3페이지 (꾸준한 조회)
     */
    WARM(10, TimeUnit.MINUTES, false, CacheUpdateStrategy.CACHE_ASIDE),

    /**
     * Cold: 캐시 미사용
     * 4페이지 이상 (거의 조회 안 됨)
     */
    COLD(0, TimeUnit.MINUTES, false, CacheUpdateStrategy.NO_CACHE);

    private final long ttl;
    private final TimeUnit timeUnit;
    private final boolean useBatchUpdate;
    private final CacheUpdateStrategy updateStrategy;

    /**
     * 캐시 사용 여부
     */
    public boolean shouldCache() {
        return this != COLD;
    }

    /**
     * 배치 갱신 사용 여부
     */
    public boolean shouldUseBatchUpdate() {
        return useBatchUpdate;
    }
}
