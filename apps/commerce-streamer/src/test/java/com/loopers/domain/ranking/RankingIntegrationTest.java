package com.loopers.domain.ranking;

import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingIntegrationTest {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RankingWeight rankingWeight;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private final LocalDate testDate = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        redisCleanUp.truncateAll();
    }

    @AfterAll
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("랭킹 점수 누적 테스트")
    class ScoreAccumulation {

        @Test
        @DisplayName("동일 상품에 대한 여러 이벤트가 누적된다")
        void shouldAccumulateScoresForSameProduct() {
            // given
            Long productId = 100L;

            // when
            rankingService.incrementViewScore(productId, testDate);
            rankingService.incrementViewScore(productId, testDate);
            rankingService.updateLikeScore(productId, true, testDate);

            // then
            String key = RankingKey.daily(testDate);
            Double score = redisTemplate.opsForZSet().score(key, productId.toString());

            // 예상: view(0.1) * 2 + like(0.2) * 1 = 0.4
            assertThat(score).isNotNull();
            assertThat(score).isGreaterThanOrEqualTo(0.4);
        }

        @Test
        @DisplayName("좋아요 취소 시 점수가 감소한다")
        void shouldDecreaseScoreWhenUnliked() {
            // given
            Long productId = 100L;
            rankingService.updateLikeScore(productId, true, testDate);

            String key = RankingKey.daily(testDate);
            Double beforeScore = redisTemplate.opsForZSet().score(key, productId.toString());

            // when
            rankingService.updateLikeScore(productId, false, testDate);

            // then
            Double afterScore = redisTemplate.opsForZSet().score(key, productId.toString());
            assertThat(afterScore).isLessThan(beforeScore);
        }
    }

    @Nested
    @DisplayName("키 TTL 테스트")
    class KeyTtl {

        @Test
        @DisplayName("새로운 랭킹 키 생성 시 TTL이 설정된다")
        void shouldSetTtlWhenKeyCreated() {
            // given
            Long productId = 100L;
            String key = RankingKey.daily(testDate);

            // when
            rankingService.incrementViewScore(productId, testDate);

            // then
            Long ttl = redisTemplate.getExpire(key);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Carry-Over 테스트")
    class CarryOver {

        @Test
        @DisplayName("전날 점수의 일부가 다음날로 복사된다")
        void shouldCopyScoresToNextDay() {
            // given
            Long productId = 100L;
            LocalDate today = testDate;
            LocalDate tomorrow = testDate.plusDays(1);

            // 오늘 점수 생성
            rankingService.incrementOrderScore(productId, 10, today);

            String todayKey = RankingKey.daily(today);
            Double todayScore = redisTemplate.opsForZSet().score(todayKey, productId.toString());

            // when
            rankingService.carryOverScores(today, tomorrow, 0.1);

            // then
            String tomorrowKey = RankingKey.daily(tomorrow);
            Double tomorrowScore = redisTemplate.opsForZSet().score(tomorrowKey, productId.toString());

            assertThat(tomorrowScore).isNotNull();
            assertThat(tomorrowScore).isCloseTo(todayScore * 0.1, org.assertj.core.api.Assertions.within(0.01));
        }

        @Test
        @DisplayName("이미 데이터가 있는 키에는 carry-over 하지 않는다")
        void shouldNotOverwriteExistingData() {
            // given
            Long productId = 100L;
            LocalDate today = testDate;
            LocalDate tomorrow = testDate.plusDays(1);

            // 오늘 점수 생성
            rankingService.incrementOrderScore(productId, 10, today);

            // 내일 키에 미리 데이터 생성
            rankingService.incrementViewScore(productId, tomorrow);
            String tomorrowKey = RankingKey.daily(tomorrow);
            Double existingScore = redisTemplate.opsForZSet().score(tomorrowKey, productId.toString());

            // when
            rankingService.carryOverScores(today, tomorrow, 0.1);

            // then (점수가 변경되지 않아야 함)
            Double afterScore = redisTemplate.opsForZSet().score(tomorrowKey, productId.toString());
            assertThat(afterScore).isEqualTo(existingScore);
        }
    }

    @Nested
    @DisplayName("동적 가중치 테스트")
    class DynamicWeight {

        @Test
        @DisplayName("Redis에서 가중치를 동적으로 업데이트하고 조회할 수 있다")
        void shouldUpdateAndRetrieveWeightsDynamically() {
            // given
            double newViewWeight = 0.15;
            double newLikeWeight = 0.25;
            double newOrderWeight = 0.6;

            // when
            rankingWeight.updateAllWeights(newViewWeight, newLikeWeight, newOrderWeight);

            // then
            assertThat(rankingWeight.getViewWeight()).isEqualTo(newViewWeight);
            assertThat(rankingWeight.getLikeWeight()).isEqualTo(newLikeWeight);
            assertThat(rankingWeight.getOrderWeight()).isEqualTo(newOrderWeight);
        }

        @Test
        @DisplayName("가중치 초기화 시 기본값으로 복원된다")
        void shouldResetToDefaultWeights() {
            // given
            rankingWeight.updateAllWeights(0.5, 0.5, 0.5);

            // when
            rankingWeight.resetToDefault();

            // then
            assertThat(rankingWeight.getViewWeight()).isEqualTo(0.1);
            assertThat(rankingWeight.getLikeWeight()).isEqualTo(0.2);
            assertThat(rankingWeight.getOrderWeight()).isEqualTo(0.7);
        }
    }
}
