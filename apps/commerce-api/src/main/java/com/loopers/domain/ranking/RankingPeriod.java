package com.loopers.domain.ranking;

/**
 * 랭킹 조회 기간 타입
 */
public enum RankingPeriod {
    /**
     * 일간 랭킹 (Redis ZSET 기반)
     */
    DAILY,
    
    /**
     * 주간 랭킹 (mv_product_rank_weekly 기반)
     */
    WEEKLY,
    
    /**
     * 월간 랭킹 (mv_product_rank_monthly 기반)
     */
    MONTHLY
}