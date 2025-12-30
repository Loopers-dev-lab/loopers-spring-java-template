package com.loopers.domain.ranking;

import com.loopers.support.IntegrationTest;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RankingServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private RankingService rankingService;

    @Autowired
    @Qualifier("redisTemplateMaster")
    private RedisTemplate<String, String> redisTemplateMaster;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @BeforeEach
    void setUp() {
        redisCleanUp.truncateAll();
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("랭킹 조회")
    @Nested
    class GetRanking {

        @DisplayName("랭킹 데이터가 있으면 정상적으로 조회된다.")
        @Test
        void returnsRankings_whenDataExists() {
            // arrange
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

            zSetOps.add(key, "1", 100.0);
            zSetOps.add(key, "2", 50.0);
            zSetOps.add(key, "3", 30.0);

            // act
            List<Ranking> result = rankingService.getRanking(date, 1, 20);

            // assert
            assertThat(result).hasSize(3);
            assertThat(result.get(0).productId()).isEqualTo(1L);
            assertThat(result.get(0).rank()).isEqualTo(1L);
            assertThat(result.get(0).score()).isEqualTo(100.0);
            assertThat(result.get(1).productId()).isEqualTo(2L);
            assertThat(result.get(1).rank()).isEqualTo(2L);
            assertThat(result.get(1).score()).isEqualTo(50.0);
            assertThat(result.get(2).productId()).isEqualTo(3L);
            assertThat(result.get(2).rank()).isEqualTo(3L);
            assertThat(result.get(2).score()).isEqualTo(30.0);
        }

        @DisplayName("페이징이 정상적으로 동작한다.")
        @Test
        void returnsPaginatedResults() {
            // arrange
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

            zSetOps.add(key, "1", 100.0);
            zSetOps.add(key, "2", 90.0);
            zSetOps.add(key, "3", 80.0);
            zSetOps.add(key, "4", 70.0);
            zSetOps.add(key, "5", 60.0);

            // act - 첫 페이지
            List<Ranking> page1 = rankingService.getRanking(date, 1, 2);

            // assert
            assertThat(page1).hasSize(2);
            assertThat(page1.get(0).productId()).isEqualTo(1L);
            assertThat(page1.get(0).rank()).isEqualTo(1L);
            assertThat(page1.get(1).productId()).isEqualTo(2L);
            assertThat(page1.get(1).rank()).isEqualTo(2L);

            // act - 두 번째 페이지
            List<Ranking> page2 = rankingService.getRanking(date, 2, 2);

            // assert
            assertThat(page2).hasSize(2);
            assertThat(page2.get(0).productId()).isEqualTo(3L);
            assertThat(page2.get(0).rank()).isEqualTo(3L);
            assertThat(page2.get(1).productId()).isEqualTo(4L);
            assertThat(page2.get(1).rank()).isEqualTo(4L);

            // act - 세 번째 페이지
            List<Ranking> page3 = rankingService.getRanking(date, 3, 2);

            // assert
            assertThat(page3).hasSize(1);
            assertThat(page3.get(0).productId()).isEqualTo(5L);
            assertThat(page3.get(0).rank()).isEqualTo(5L);
        }

        @DisplayName("랭킹 데이터가 없으면 빈 리스트를 반환한다.")
        @Test
        void returnsEmptyList_whenNoDataExists() {
            // arrange
            LocalDate date = LocalDate.now();

            // act
            List<Ranking> result = rankingService.getRanking(date, 1, 20);

            // assert
            assertThat(result).isEmpty();
        }

        @DisplayName("순위는 1부터 시작한다.")
        @Test
        void startsRankFromOne() {
            // arrange
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

            zSetOps.add(key, "1", 100.0);

            // act
            List<Ranking> result = rankingService.getRanking(date, 1, 20);

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).rank()).isEqualTo(1L);
        }
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}

