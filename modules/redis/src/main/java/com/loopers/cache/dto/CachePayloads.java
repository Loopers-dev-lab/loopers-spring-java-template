package com.loopers.cache.dto;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 23.
 */
public class CachePayloads {
    /**
     * 랭킹 아이템
     */
    public record RankingItem(
            long rank,
            Long productId,
            Double score
    ) {}

    public record RankingScore(
            Long productId,
            EventType eventType,
            double score,
            long occurredAtEpochMillis
    ) {

        public enum EventType {
            PRODUCT_VIEW(0.1),    // 조회: Weight = 0.1, Score = 1
            LIKE_ACTION(0.2),     // 좋아요: Weight = 0.2, Score = 1
            PAYMENT_SUCCESS(0.6); // 주문: Weight = 0.6, Score = Math.log((단가 * 수량) + 1)

            private final double weight;

            EventType(double weight) {
                this.weight = weight;
            }

            public double getWeight() {
                return weight;
            }
        }

    }
}
