package com.loopers.infrastructure.cache;

import com.loopers.domain.ranking.RankingItem;
import com.loopers.support.IntegrationTest;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RankingCacheServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private RankingCacheService rankingCacheService;

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

    @DisplayName("랭킹 범위 조회")
    @Nested
    class GetRankingRange {

        @DisplayName("랭킹 데이터가 있으면 정상적으로 조회된다.")
        @Test
        void returnsRankingItems_whenDataExists() {
            // arrange
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

            zSetOps.add(key, "1", 100.0);
            zSetOps.add(key, "2", 50.0);
            zSetOps.add(key, "3", 30.0);

            // act
            List<RankingItem> result = rankingCacheService.getRankingRange(date, 0, 2);

            // assert
            assertThat(result).hasSize(3);
            assertThat(result.get(0).productId()).isEqualTo(1L);
            assertThat(result.get(0).score()).isEqualTo(100.0);
            assertThat(result.get(1).productId()).isEqualTo(2L);
            assertThat(result.get(1).score()).isEqualTo(50.0);
            assertThat(result.get(2).productId()).isEqualTo(3L);
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

            // act - 첫 페이지 (0-1)
            List<RankingItem> page1 = rankingCacheService.getRankingRange(date, 0, 1);

            // assert
            assertThat(page1).hasSize(2);
            assertThat(page1.get(0).productId()).isEqualTo(1L);
            assertThat(page1.get(1).productId()).isEqualTo(2L);

            // act - 두 번째 페이지 (2-3)
            List<RankingItem> page2 = rankingCacheService.getRankingRange(date, 2, 3);

            // assert
            assertThat(page2).hasSize(2);
            assertThat(page2.get(0).productId()).isEqualTo(3L);
            assertThat(page2.get(1).productId()).isEqualTo(4L);
        }

        @DisplayName("랭킹 데이터가 없으면 빈 리스트를 반환한다.")
        @Test
        void returnsEmptyList_whenNoDataExists() {
            // arrange
            LocalDate date = LocalDate.now();

            // act
            List<RankingItem> result = rankingCacheService.getRankingRange(date, 0, 10);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("상품 랭킹 조회")
    @Nested
    class GetProductRank {

        @DisplayName("랭킹에 있는 상품의 순위를 반환한다.")
        @Test
        void returnsRank_whenProductExistsInRanking() {
            // arrange
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

            zSetOps.add(key, "1", 100.0);
            zSetOps.add(key, "2", 50.0);
            zSetOps.add(key, "3", 30.0);

            // act
            Long rank1 = rankingCacheService.getProductRank(date, 1L);
            Long rank2 = rankingCacheService.getProductRank(date, 2L);
            Long rank3 = rankingCacheService.getProductRank(date, 3L);

            // assert
            assertThat(rank1).isEqualTo(1L); // 1등
            assertThat(rank2).isEqualTo(2L); // 2등
            assertThat(rank3).isEqualTo(3L); // 3등
        }

        @DisplayName("랭킹에 없는 상품은 null을 반환한다.")
        @Test
        void returnsNull_whenProductNotInRanking() {
            // arrange
            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

            zSetOps.add(key, "1", 100.0);

            // act
            Long rank = rankingCacheService.getProductRank(date, 999L);

            // assert
            assertThat(rank).isNull();
        }
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}

