package com.loopers.cache.dto;

import java.math.BigDecimal;
import java.util.Objects;

import lombok.Getter;

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

        @Getter
        public enum EventType {
            PRODUCT_VIEW(0.1),    // 조회: Weight = 0.1, Score = 1
            LIKE_ACTION(0.2),     // 좋아요: Weight = 0.2, Score = 1
            PAYMENT_SUCCESS(0.6); // 주문: Weight = 0.6, Score = price * amount (정규화 시에는 log 적용도 가능)

            private final double weight;

            EventType(double weight) {
                this.weight = weight;
            }
        }
        
        /**
         * 발생 시각으로부터 날짜 추출
         */
        public java.time.LocalDate getEventDate() {
            return java.time.Instant.ofEpochMilli(occurredAtEpochMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        
        /**
         * 조회 이벤트 생성
         */
        public static RankingScore forProductView(Long productId, long occurredAt) {
            return new RankingScore(productId, EventType.PRODUCT_VIEW, 1.0, occurredAt);
        }
        
        /**
         * 좋아요 이벤트 생성
         */
        public static RankingScore forLikeAction(Long productId, long occurredAt) {
            return new RankingScore(productId, EventType.LIKE_ACTION, 1.0, occurredAt);
        }
        
        /**
         * 주문 이벤트 생성 메소드 (가격 * 수량 기반, 로그 정규화)
         */
        public static RankingScore forPaymentSuccess(Long productId, BigDecimal totalPrice, long occurredAt) {
            Objects.requireNonNull(totalPrice);
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
