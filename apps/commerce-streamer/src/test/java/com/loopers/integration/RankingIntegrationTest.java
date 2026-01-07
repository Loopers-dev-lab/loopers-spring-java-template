package com.loopers.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.awaitility.Awaitility.await;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.cache.CacheKeyGenerator;
import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingItem;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;
import com.loopers.utils.RedisCleanUp;

/**
 * 랭킹 시스템 통합 테스트
 * <p>
 * Kafka 이벤트 → Redis ZSET 적재 → 랭킹 조회 E2E 테스트
 *
 * @author hyunjikoh
 * @since 2025.12.26
 */
@SpringBootTest
class RankingIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private RankingRedisService rankingRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    private LocalDate today;
    private String todayRankingKey;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        todayRankingKey = cacheKeyGenerator.generateDailyRankingKey(today);

        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("Kafka 이벤트 → Redis ZSET 적재 테스트")
    class KafkaToRedisTest {

        @Test
        @DisplayName("PRODUCT_VIEW 이벤트가 랭킹 점수로 적재되어야 한다")
        void shouldStoreProductViewAsRankingScore() throws Exception {
            // Given
            Long productId = 1001L;
            String eventId = "ranking-view-test-" + System.currentTimeMillis();

            ProductViewPayloadV1 payload = new ProductViewPayloadV1(productId, 100L);
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    eventId, "PRODUCT_VIEW", "v1", System.currentTimeMillis(), payloadJson
            );

            // When
            kafkaTemplate.send("catalog-events", envelope);

            // Then - 랭킹 점수가 적재되어야 함 (Weight 0.1 * Score 1 = 0.1)
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        RankingItem ranking = rankingRedisService.getProductRanking(today, productId);
                        assertThat(ranking).isNotNull();
                        assertThat(ranking.productId()).isEqualTo(productId);
                        assertThat(ranking.score()).isCloseTo(0.1, offset(0.01));
                    });
        }

        @Test
        @DisplayName("LIKE_ACTION 이벤트가 랭킹 점수로 적재되어야 한다")
        void shouldStoreLikeActionAsRankingScore() throws Exception {
            // Given
            Long productId = 1002L;
            String eventId = "ranking-like-test-" + System.currentTimeMillis();

            LikeActionPayloadV1 payload = new LikeActionPayloadV1(productId, 100L, "LIKE");
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    eventId, "LIKE_ACTION", "v1", System.currentTimeMillis(), payloadJson
            );

            // When
            kafkaTemplate.send("catalog-events", envelope);

            // Then - 랭킹 점수가 적재되어야 함 (Weight 0.2 * Score 1 = 0.2)
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        RankingItem ranking = rankingRedisService.getProductRanking(today, productId);
                        assertThat(ranking).isNotNull();
                        assertThat(ranking.score()).isCloseTo(0.2, offset(0.01));
                    });
        }

        @Test
        @DisplayName("PAYMENT_SUCCESS 이벤트가 로그 정규화된 랭킹 점수로 적재되어야 한다")
        void shouldStorePaymentSuccessAsLogNormalizedScore() throws Exception {
            // Given
            Long productId = 1003L;
            String eventId = "ranking-payment-test-" + System.currentTimeMillis();
            BigDecimal totalPrice = BigDecimal.valueOf(10000);

            PaymentSuccessPayloadV1 payload = new PaymentSuccessPayloadV1(
                    1L, 1L, 100L, productId, 2, BigDecimal.valueOf(5000), totalPrice
            );
            String payloadJson = objectMapper.writeValueAsString(payload);

            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    eventId, "PAYMENT_SUCCESS", "v1", System.currentTimeMillis(), payloadJson
            );

            // When
            kafkaTemplate.send("order-events", envelope);

            // Then - 로그 정규화된 점수가 적재되어야 함
            // Weight 0.6 * log(10001) ≈ 5.53
            double expectedScore = 0.6 * Math.log(10001);

            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        RankingItem ranking = rankingRedisService.getProductRanking(today, productId);
                        assertThat(ranking).isNotNull();
                        assertThat(ranking.score()).isCloseTo(expectedScore, offset(0.1));
                    });
        }

        @Test
        @DisplayName("여러 이벤트가 동일 상품에 대해 점수가 누적되어야 한다")
        void shouldAccumulateScoresForSameProduct() throws Exception {
            // Given
            Long productId = 1004L;
            long baseTime = System.currentTimeMillis();

            // 조회 3회 + 좋아요 2회 = 0.1*3 + 0.2*2 = 0.7
            for (int i = 0; i < 3; i++) {
                ProductViewPayloadV1 viewPayload = new ProductViewPayloadV1(productId, 100L);
                DomainEventEnvelope viewEnvelope = new DomainEventEnvelope(
                        "view-" + productId + "-" + i + "-" + baseTime,
                        "PRODUCT_VIEW", "v1", baseTime + i,
                        objectMapper.writeValueAsString(viewPayload)
                );
                kafkaTemplate.send("catalog-events", viewEnvelope);
            }

            for (int i = 0; i < 2; i++) {
                LikeActionPayloadV1 likePayload = new LikeActionPayloadV1(productId, (long) (100 + i), "LIKE");
                DomainEventEnvelope likeEnvelope = new DomainEventEnvelope(
                        "like-" + productId + "-" + i + "-" + baseTime,
                        "LIKE_ACTION", "v1", baseTime + 10 + i,
                        objectMapper.writeValueAsString(likePayload)
                );
                kafkaTemplate.send("catalog-events", likeEnvelope);
            }

            // Then - 점수가 누적되어야 함
            double expectedScore = 0.1 * 3 + 0.2 * 2; // 0.7

            await().atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> {
                        RankingItem ranking = rankingRedisService.getProductRanking(today, productId);
                        assertThat(ranking).isNotNull();
                        assertThat(ranking.score()).isCloseTo(expectedScore, offset(0.1));
                    });
        }
    }

    @Nested
    @DisplayName("랭킹 조회 테스트")
    class RankingQueryTest {

        @Test
        @DisplayName("랭킹 순서대로 조회되어야 한다")
        void shouldReturnRankingsInOrder() throws Exception {
            // Given - 점수가 다른 3개 상품 등록
            Long product1 = 2001L; // 높은 점수
            Long product2 = 2002L; // 중간 점수
            Long product3 = 2003L; // 낮은 점수

            // product1: 결제 (높은 점수)
            PaymentSuccessPayloadV1 paymentPayload = new PaymentSuccessPayloadV1(
                    1L, 1L, 100L, product1, 1, BigDecimal.valueOf(50000), BigDecimal.valueOf(50000)
            );
            kafkaTemplate.send("order-events", new DomainEventEnvelope(
                    "order-" + product1 + "-" + System.currentTimeMillis(),
                    "PAYMENT_SUCCESS", "v1", System.currentTimeMillis(),
                    objectMapper.writeValueAsString(paymentPayload)
            ));

            // product2: 좋아요 (중간 점수)
            LikeActionPayloadV1 likePayload = new LikeActionPayloadV1(product2, 100L, "LIKE");
            kafkaTemplate.send("catalog-events", new DomainEventEnvelope(
                    "like-" + product2 + "-" + System.currentTimeMillis(),
                    "LIKE_ACTION", "v1", System.currentTimeMillis(),
                    objectMapper.writeValueAsString(likePayload)
            ));

            // product3: 조회 (낮은 점수)
            ProductViewPayloadV1 viewPayload = new ProductViewPayloadV1(product3, 100L);
            kafkaTemplate.send("catalog-events", new DomainEventEnvelope(
                    "view-" + product3 + "-" + System.currentTimeMillis(),
                    "PRODUCT_VIEW", "v1", System.currentTimeMillis(),
                    objectMapper.writeValueAsString(viewPayload)
            ));

            // Then - 점수 높은 순으로 정렬되어야 함
            await().atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> {
                        List<RankingItem> rankings = rankingRedisService.getRanking(today, 1, 10);
                        assertThat(rankings).hasSizeGreaterThanOrEqualTo(3);

                        // 첫 번째가 가장 높은 점수 (결제)
                        assertThat(rankings.get(0).productId()).isEqualTo(product1);
                        // 두 번째가 중간 점수 (좋아요)
                        assertThat(rankings.get(1).productId()).isEqualTo(product2);
                        // 세 번째가 낮은 점수 (조회)
                        assertThat(rankings.get(2).productId()).isEqualTo(product3);
                    });
        }
    }

    @Nested
    @DisplayName("Score Carry-Over 테스트")
    class CarryOverTest {

        @Test
        @DisplayName("전날 점수의 일부가 다음 날로 이월되어야 한다")
        void shouldCarryOverScoresToNextDay() {
            // Given - 오늘 랭킹 데이터 직접 추가
            Long productId = 3001L;
            double originalScore = 100.0;

            redisTemplate.opsForZSet().add(todayRankingKey, productId.toString(), originalScore);

            LocalDate tomorrow = today.plusDays(1);
            String tomorrowKey = cacheKeyGenerator.generateDailyRankingKey(tomorrow);
            redisTemplate.delete(tomorrowKey); // 내일 키 정리

            // When - Carry-Over 실행 (10%)
            long carryOverCount = rankingRedisService.carryOverScores(today, tomorrow, 0.1);

            // Then
            assertThat(carryOverCount).isEqualTo(1);

            Double tomorrowScore = redisTemplate.opsForZSet().score(tomorrowKey, productId.toString());
            assertThat(tomorrowScore).isNotNull();
            assertThat(tomorrowScore).isCloseTo(10.0, offset(0.01)); // 100 * 0.1

            // Cleanup
            redisTemplate.delete(tomorrowKey);
        }

        @Test
        @DisplayName("원본 데이터가 없으면 Carry-Over를 스킵해야 한다")
        void shouldSkipCarryOverWhenNoSourceData() {
            // Given
            LocalDate emptyDate = today.minusDays(10);
            LocalDate targetDate = emptyDate.plusDays(1);

            String emptyKey = cacheKeyGenerator.generateDailyRankingKey(emptyDate);
            redisTemplate.delete(emptyKey); // 확실히 비어있게

            // When
            long carryOverCount = rankingRedisService.carryOverScores(emptyDate, targetDate, 0.1);

            // Then
            assertThat(carryOverCount).isEqualTo(0);
        }
    }
}
