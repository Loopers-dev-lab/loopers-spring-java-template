package com.loopers.domain.ranking;

import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
class RankingServiceIntegrationTest {

    @Autowired
    private RankingService rankingService;

    @Autowired
    @Qualifier("redisTemplateMaster")
    private RedisTemplate<String, String> redisTemplateMaster;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("랭킹 점수 증가")
    @Nested
    class IncrementScore {

        @DisplayName("좋아요 이벤트 발생 시 점수가 적절하게 증가한다.")
        @Test
        void incrementsScore_whenLikeEventOccurs() {
            // arrange
            Long productId = 1L;
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);

            // act
            rankingService.incrementScore(productId, RankingWeight.LIKE);

            // assert
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            Double score = zSetOps.score(key, String.valueOf(productId));
            assertThat(score).isNotNull();
            assertThat(score).isCloseTo(RankingWeight.LIKE.getWeight(), within(0.001));

            // TTL 확인
            Long ttl = redisTemplateMaster.getExpire(key);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(0);
            assertThat(ttl).isLessThanOrEqualTo(2 * 24 * 60 * 60); // 2일
        }

        @DisplayName("조회수 이벤트 발생 시 점수가 적절하게 증가한다.")
        @Test
        void incrementsScore_whenViewEventOccurs() {
            // arrange
            Long productId = 2L;
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);

            // act
            rankingService.incrementScore(productId, RankingWeight.VIEW);

            // assert
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            Double score = zSetOps.score(key, String.valueOf(productId));
            assertThat(score).isNotNull();
            assertThat(score).isCloseTo(RankingWeight.VIEW.getWeight(), within(0.001));
        }

        @DisplayName("주문 생성 이벤트 발생 시 점수가 적절하게 증가한다.")
        @Test
        void incrementsScore_whenOrderCreatedEventOccurs() {
            // arrange
            Long productId = 3L;
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);

            // act
            rankingService.incrementScore(productId, RankingWeight.ORDER_CREATED);

            // assert
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            Double score = zSetOps.score(key, String.valueOf(productId));
            assertThat(score).isNotNull();
            assertThat(score).isCloseTo(RankingWeight.ORDER_CREATED.getWeight(), within(0.001));
        }

        @DisplayName("여러 번 점수를 증가시키면 누적된다.")
        @Test
        void accumulatesScore_whenMultipleIncrements() {
            // arrange
            Long productId = 4L;
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);

            // act
            rankingService.incrementScore(productId, RankingWeight.LIKE);
            rankingService.incrementScore(productId, RankingWeight.VIEW);
            rankingService.incrementScore(productId, RankingWeight.ORDER_CREATED);

            // assert
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            Double score = zSetOps.score(key, String.valueOf(productId));
            assertThat(score).isNotNull();
            double expectedScore = RankingWeight.LIKE.getWeight() +
                    RankingWeight.VIEW.getWeight() +
                    RankingWeight.ORDER_CREATED.getWeight();
            assertThat(score).isCloseTo(expectedScore, within(0.001));
        }

        @DisplayName("날짜별로 다른 키를 사용한다.")
        @Test
        void usesDifferentKeys_forDifferentDates() {
            // arrange
            Long productId = 5L;
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            // act
            rankingService.incrementScore(productId, RankingWeight.LIKE);

            // assert
            String todayKey = getRankingKey(today);
            String tomorrowKey = getRankingKey(tomorrow);

            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            Double todayScore = zSetOps.score(todayKey, String.valueOf(productId));
            Double tomorrowScore = zSetOps.score(tomorrowKey, String.valueOf(productId));

            assertThat(todayScore).isNotNull();
            assertThat(tomorrowScore).isNull(); // 내일 키에는 데이터가 없음
        }
    }

    @DisplayName("랭킹 점수 Carry-Over")
    @Nested
    class CarryOverScore {

        @DisplayName("전날 점수를 다음 날로 carry-over한다.")
        @Test
        void carriesOverScore_fromPreviousDayToNextDay() {
            // arrange
            Long productId1 = 10L;
            Long productId2 = 20L;
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate today = LocalDate.now();

            String yesterdayKey = getRankingKey(yesterday);
            String todayKey = getRankingKey(today);

            // 전날 데이터 설정
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            zSetOps.add(yesterdayKey, String.valueOf(productId1), 100.0);
            zSetOps.add(yesterdayKey, String.valueOf(productId2), 50.0);

            // act
            rankingService.carryOverScore(yesterday, today);

            // assert
            Double todayScore1 = zSetOps.score(todayKey, String.valueOf(productId1));
            Double todayScore2 = zSetOps.score(todayKey, String.valueOf(productId2));

            assertThat(todayScore1).isNotNull();
            assertThat(todayScore2).isNotNull();

            // carry-over weight (0.1)가 적용된 점수
            assertThat(todayScore1).isCloseTo(100.0 * RankingWeight.CARRY_OVER.getWeight(), within(0.001));
            assertThat(todayScore2).isCloseTo(50.0 * RankingWeight.CARRY_OVER.getWeight(), within(0.001));

            // TTL 확인
            Long ttl = redisTemplateMaster.getExpire(todayKey);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(0);
        }

        @DisplayName("carry-over 후에도 원본 데이터는 유지된다.")
        @Test
        void preservesOriginalData_afterCarryOver() {
            // arrange
            Long productId = 30L;
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate today = LocalDate.now();

            String yesterdayKey = getRankingKey(yesterday);
            String todayKey = getRankingKey(today);

            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            zSetOps.add(yesterdayKey, String.valueOf(productId), 200.0);

            // act
            rankingService.carryOverScore(yesterday, today);

            // assert
            Double yesterdayScore = zSetOps.score(yesterdayKey, String.valueOf(productId));
            Double todayScore = zSetOps.score(todayKey, String.valueOf(productId));

            assertThat(yesterdayScore).isCloseTo(200.0, within(0.001));
            assertThat(todayScore).isCloseTo(200.0 * RankingWeight.CARRY_OVER.getWeight(), within(0.001));
        }
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}

