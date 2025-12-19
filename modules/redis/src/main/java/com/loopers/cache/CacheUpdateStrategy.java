package com.loopers.cache;

/**
 * 캐시 갱신 전략
 *
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
public enum CacheUpdateStrategy {
    /**
     * 배치 갱신: 주기적으로 캐시를 갱신
     */
    BATCH_REFRESH,

    /**
     * Cache-Aside: 캐시 미스 시 DB 조회 후 캐시 저장
     */
    CACHE_ASIDE,

    /**
     * 캐시 미사용
     */
    NO_CACHE
}
