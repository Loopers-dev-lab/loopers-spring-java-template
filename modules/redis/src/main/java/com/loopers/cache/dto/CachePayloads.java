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
        
        /**
         * 조회 이벤트 점수 생성
         */
        public static RankingScore forProductView(Long productId, long occurredAt) {
            return new RankingScore(productId, EventType.PRODUCT_VIEW, 1.0, occurredAt);
        }
        
        /**
         * 좋아요 이벤트 점수 생성
         */
        public static RankingScore forLikeAction(Long productId, long occurredAt) {
            return new RankingScore(productId, EventType.LIKE_ACTION, 1.0, occurredAt);
        }
        
        /**
         * 주문 이벤트 점수 생성 (가격 * 수량 기반, 로그 정규화)
         */
        public static RankingScore forPaymentSuccess(Long productId, java.math.BigDecimal totalPrice, long occurredAt) {
            // 로그 정규화 적용하여 극값 방지
            // Math.log(x + 1)을 사용하여 0원일 때도 안전하게 처리
            double normalizedScore = Math.log(totalPrice.doubleValue() + 1);
            return new RankingScore(productId, EventType.PAYMENT_SUCCESS, normalizedScore, occurredAt);
        }
        
        /**
         * 최종 가중 점수 계산
         */
        public double getWeightedScore() {
            return eventType.getWeight() * score;
        }

    }
}
