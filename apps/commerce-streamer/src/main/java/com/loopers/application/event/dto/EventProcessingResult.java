package com.loopers.application.event.dto;

import com.loopers.cache.dto.CachePayloads.RankingScore;

/**
 * 이벤트 처리 결과 DTO
 *
 * @author hyunjikoh
 * @since 2025.12.26
 */
public class EventProcessingResult {

    /**
     * 카탈로그 이벤트 처리 결과
     */
    public record CatalogEventResult(
            boolean processed,
            RankingScore rankingScore
    ) {
        public static CatalogEventResult notProcessed() {
            return new CatalogEventResult(false, null);
        }

        public static CatalogEventResult processed(RankingScore rankingScore) {
            return new CatalogEventResult(true, rankingScore);
        }
    }

    /**
     * 주문 이벤트 처리 결과
     */
    public record OrderEventResult(
            boolean processed,
            RankingScore rankingScore
    ) {
        public static OrderEventResult notProcessed() {
            return new OrderEventResult(false, null);
        }

        public static OrderEventResult processed(RankingScore rankingScore) {
            return new OrderEventResult(true, rankingScore);
        }
    }
}
