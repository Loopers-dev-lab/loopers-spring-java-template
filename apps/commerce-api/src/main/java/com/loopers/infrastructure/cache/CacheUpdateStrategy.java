package com.loopers.infrastructure.cache;

/**
 * 캐시 갱신 전략
 */
public enum CacheUpdateStrategy {

    /**
     * 배치 갱신 (주기적으로 미리 갱신)
     * - 적용: Hot 데이터 (상품 목록, 인기 상품 등)
     */
    BATCH_REFRESH,

    /**
     * Cache-Aside (조회 시 캐시 미스 발생 시 DB에서 로드)
     * - 적용: Warm 데이터 (개별 상품 상세정보 등)
     */
    CACHE_ASIDE,

    /**
     * 캐시 미사용 (항상 DB 직접 조회)
     * - 적용: Cold 데이터 (조회 빈도 낮은 데이터)
     */
    NO_CACHE
}
