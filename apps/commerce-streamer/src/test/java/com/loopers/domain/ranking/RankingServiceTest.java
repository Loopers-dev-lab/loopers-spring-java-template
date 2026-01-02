package com.loopers.domain.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingItem;
import com.loopers.cache.dto.CachePayloads.RankingScore;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.EventDeserializer;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;

/**
 * 랭킹 서비스 단위 테스트
 *
 * @author hyunjikoh
 * @since 2025.12.26
 */
@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RankingRedisService rankingRedisService;

    @Mock
    private EventDeserializer eventDeserializer;

    @InjectMocks
    private RankingService rankingService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("랭킹 점수 생성 테스트")
    class GenerateRankingScoreTest {

        @Test
        @DisplayName("PRODUCT_VIEW 이벤트에서 랭킹 점수를 생성해야 한다")
        void shouldGenerateScoreForProductView() throws Exception {
            // Given
            Long productId = 1L;
            long occurredAt = System.currentTimeMillis();

            ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, 100L);
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    "event-1", "PRODUCT_VIEW", "v1", occurredAt, payloadJson
            );

            when(eventDeserializer.deserializeProductView(payloadJson)).thenReturn(payload);

            // When
            RankingScore score = rankingService.generateRankingScore(envelope);

            // Then
            assertThat(score).isNotNull();
            assertThat(score.productId()).isEqualTo(productId);
            assertThat(score.eventType()).isEqualTo(RankingScore.EventType.PRODUCT_VIEW);
            assertThat(score.score()).isEqualTo(1.0);
            assertThat(score.getWeightedScore()).isEqualTo(0.1); // 0.1 * 1.0
        }

        @Test
        @DisplayName("LIKE_ACTION 이벤트(좋아요)에서 랭킹 점수를 생성해야 한다")
        void shouldGenerateScoreForLikeAction() throws Exception {
            // Given
            Long productId = 2L;
            long occurredAt = System.currentTimeMillis();

            LikeActionPayloadV1 payload = new LikeActionPayloadV1(productId, 100L, "LIKE");
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    "event-2", "LIKE_ACTION", "v1", occurredAt, payloadJson
            );

            when(eventDeserializer.deserializeLikeAction(payloadJson)).thenReturn(payload);

            // When
            RankingScore score = rankingService.generateRankingScore(envelope);

            // Then
            assertThat(score).isNotNull();
            assertThat(score.productId()).isEqualTo(productId);
            assertThat(score.eventType()).isEqualTo(RankingScore.EventType.LIKE_ACTION);
            assertThat(score.getWeightedScore()).isEqualTo(0.2); // 0.2 * 1.0
        }

        @Test
        @DisplayName("LIKE_ACTION 이벤트(좋아요 취소)는 랭킹 점수를 생성하지 않아야 한다")
        void shouldNotGenerateScoreForUnlike() throws Exception {
            // Given
            Long productId = 2L;
            long occurredAt = System.currentTimeMillis();

            LikeActionPayloadV1 payload = new LikeActionPayloadV1(productId, 100L, "UNLIKE");
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    "event-3", "LIKE_ACTION", "v1", occurredAt, payloadJson
            );

            when(eventDeserializer.deserializeLikeAction(payloadJson)).thenReturn(payload);

            // When
            RankingScore score = rankingService.generateRankingScore(envelope);

            // Then
            assertThat(score).isNull();
        }

        @Test
        @DisplayName("PAYMENT_SUCCESS 이벤트에서 로그 정규화된 랭킹 점수를 생성해야 한다")
        void shouldGenerateLogNormalizedScoreForPaymentSuccess() throws Exception {
            // Given
            Long productId = 3L;
            long occurredAt = System.currentTimeMillis();
            BigDecimal totalPrice = BigDecimal.valueOf(10000);

            PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(
                    1L, 1L, 100L, productId, 2, BigDecimal.valueOf(5000), totalPrice
            );
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    "event-4", "PAYMENT_SUCCESS", "v1", occurredAt, payloadJson
            );

            when(eventDeserializer.deserializePaymentSuccess(payloadJson)).thenReturn(payload);

            // When
            RankingScore score = rankingService.generateRankingScore(envelope);

            // Then
            assertThat(score).isNotNull();
            assertThat(score.productId()).isEqualTo(productId);
            assertThat(score.eventType()).isEqualTo(RankingScore.EventType.PAYMENT_SUCCESS);

            // 로그 정규화 확인: log(10000 + 1) ≈ 9.21
            double expectedScore = Math.log(10001);
            assertThat(score.score()).isCloseTo(expectedScore, org.assertj.core.data.Offset.offset(0.01));

            // 가중치 적용: 0.6 * log(10001) ≈ 5.53
            assertThat(score.getWeightedScore()).isCloseTo(0.6 * expectedScore, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("지원하지 않는 이벤트 타입은 null을 반환해야 한다")
        void shouldReturnNullForUnsupportedEventType() {
            // Given
            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    "event-5", "UNKNOWN_EVENT", "v1", System.currentTimeMillis(), "{}"
            );

            // When
            RankingScore score = rankingService.generateRankingScore(envelope);

            // Then
            assertThat(score).isNull();
        }
    }

    @Nested
    @DisplayName("랭킹 점수 배치 업데이트 테스트")
    class UpdateRankingScoresBatchTest {

        @Test
        @DisplayName("랭킹 점수 리스트를 배치로 업데이트해야 한다")
        void shouldUpdateRankingScoresInBatch() {
            // Given
            LocalDate today = LocalDate.now();
            List<RankingScore> scores = List.of(
                    RankingScore.forProductView(1L, System.currentTimeMillis()),
                    RankingScore.forLikeAction(2L, System.currentTimeMillis()),
                    RankingScore.forPaymentSuccess(3L, BigDecimal.valueOf(5000), System.currentTimeMillis())
            );

            // When
            rankingService.updateRankingScoresBatch(scores, today);

            // Then
            verify(rankingRedisService).updateRankingScoresBatch(scores, today);
        }

        @Test
        @DisplayName("빈 리스트는 업데이트하지 않아야 한다")
        void shouldNotUpdateEmptyList() {
            // Given
            List<RankingScore> emptyScores = List.of();

            // When
            rankingService.updateRankingScoresBatch(emptyScores, LocalDate.now());

            // Then
            verify(rankingRedisService, never()).updateRankingScoresBatch(any(), any());
        }

        @Test
        @DisplayName("날짜가 null이면 각 점수의 발생 날짜 기준으로 업데이트해야 한다")
        void shouldUseEventDateWhenTargetDateIsNull() {
            // Given
            List<RankingScore> scores = List.of(
                    RankingScore.forProductView(1L, System.currentTimeMillis())
            );

            // When
            rankingService.updateRankingScoresBatch(scores, null);

            // Then - targetDate가 null이면 날짜 파라미터 없이 호출
            verify(rankingRedisService).updateRankingScoresBatch(scores);
        }
    }

    @Nested
    @DisplayName("랭킹 조회 테스트")
    class GetRankingTest {

        @Test
        @DisplayName("페이징된 랭킹을 조회해야 한다")
        void shouldGetPaginatedRanking() {
            // Given
            LocalDate today = LocalDate.now();
            List<RankingItem> expectedRankings = List.of(
                    new RankingItem(1, 101L, 100.0),
                    new RankingItem(2, 102L, 90.0),
                    new RankingItem(3, 103L, 80.0)
            );

            when(rankingRedisService.getRanking(today, 1, 20)).thenReturn(expectedRankings);

            // When
            List<RankingItem> result = rankingService.getRanking(today, 1, 20);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).rank()).isEqualTo(1);
            assertThat(result.get(0).productId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("특정 상품의 랭킹을 조회해야 한다")
        void shouldGetProductRanking() {
            // Given
            LocalDate today = LocalDate.now();
            Long productId = 101L;
            RankingItem expectedRanking = new RankingItem(5, productId, 75.0);

            when(rankingRedisService.getProductRanking(today, productId)).thenReturn(expectedRanking);

            // When
            RankingItem result = rankingService.getProductRanking(productId, today);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.rank()).isEqualTo(5);
            assertThat(result.productId()).isEqualTo(productId);
            assertThat(result.score()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("랭킹에 없는 상품은 null을 반환해야 한다")
        void shouldReturnNullForUnrankedProduct() {
            // Given
            LocalDate today = LocalDate.now();
            Long productId = 999L;

            when(rankingRedisService.getProductRanking(today, productId)).thenReturn(null);

            // When
            RankingItem result = rankingService.getProductRanking(productId, today);

            // Then
            assertThat(result).isNull();
        }
    }
}
