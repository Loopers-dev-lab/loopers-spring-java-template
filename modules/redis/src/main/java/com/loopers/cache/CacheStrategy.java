package com.loopers.cache;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캐시 전략: Hot/Warm/Cold 차별화된 TTL과 갱신 방식
 * <p>
 * - Hot: 배치 갱신, TTL 30분 (인기순, 1페이지)
 * - Warm: Cache-Aside, TTL 10분 (2~3페이지)
 * - Cold: 캐시 미사용 (4페이지 이상)
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
@Getter
@RequiredArgsConstructor
public enum CacheStrategy {

    HOT(30, TimeUnit.MINUTES, true, CacheUpdateStrategy.BATCH_REFRESH),
    WARM(10, TimeUnit.MINUTES, false, CacheUpdateStrategy.CACHE_ASIDE),
    COLD(0, TimeUnit.MINUTES, false, CacheUpdateStrategy.NO_CACHE);

    private final long ttl;
    private final TimeUnit timeUnit;
    private final boolean useBatchUpdate;
    private final CacheUpdateStrategy updateStrategy;

    public boolean shouldCache() {
        return this != COLD;
    }

    public boolean shouldUseBatchUpdate() {
        return useBatchUpdate;
    }
}
