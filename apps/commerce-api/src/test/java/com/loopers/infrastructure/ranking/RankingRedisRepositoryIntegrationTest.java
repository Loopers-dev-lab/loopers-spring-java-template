package com.loopers.infrastructure.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingRepository;
import com.loopers.support.test.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RankingRedisRepositoryIntegrationTest extends IntegrationTestSupport {

  private static final String TEST_KEY = "ranking:all:20251224";

  @Autowired
  private RankingRepository rankingRepository;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @AfterEach
  void cleanUpRedis() {
    redisTemplate.delete(TEST_KEY);
  }

  @Nested
  @DisplayName("getTopN 호출 시")
  class GetTopN {

    @Test
    @DisplayName("점수가 높은 순으로 랭킹이 반환된다")
    void shouldReturnRankingsInDescendingOrder() {
      redisTemplate.opsForZSet().add(TEST_KEY, "1", 10.0);
      redisTemplate.opsForZSet().add(TEST_KEY, "2", 30.0);
      redisTemplate.opsForZSet().add(TEST_KEY, "3", 20.0);

      List<RankingEntry> result = rankingRepository.getTopN(TEST_KEY, 0, 10);

      assertThat(result)
          .hasSize(3)
          .extracting(RankingEntry::productId)
          .containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("페이지네이션이 적용된다")
    void shouldApplyPagination() {
      for (int i = 1; i <= 5; i++) {
        redisTemplate.opsForZSet().add(TEST_KEY, String.valueOf(i), i * 10.0);
      }

      List<RankingEntry> result = rankingRepository.getTopN(TEST_KEY, 1, 2);

      assertThat(result)
          .hasSize(2)
          .extracting(RankingEntry::productId)
          .containsExactly(3L, 2L);
    }

    @Test
    @DisplayName("키가 없으면 빈 리스트를 반환한다")
    void shouldReturnEmptyList_whenKeyDoesNotExist() {
      List<RankingEntry> result = rankingRepository.getTopN("non_existent_key", 0, 10);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getRank 호출 시")
  class GetRank {

    @Test
    @DisplayName("존재하는 상품의 순위를 반환한다")
    void shouldReturnRank_whenProductExists() {
      redisTemplate.opsForZSet().add(TEST_KEY, "1", 10.0);
      redisTemplate.opsForZSet().add(TEST_KEY, "2", 30.0);
      redisTemplate.opsForZSet().add(TEST_KEY, "3", 20.0);

      Integer rank = rankingRepository.getRank(TEST_KEY, 2L);

      assertThat(rank).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 상품은 null을 반환한다")
    void shouldReturnNull_whenProductDoesNotExist() {
      redisTemplate.opsForZSet().add(TEST_KEY, "1", 10.0);

      Integer rank = rankingRepository.getRank(TEST_KEY, 999L);

      assertThat(rank).isNull();
    }

    @Test
    @DisplayName("키가 없으면 null을 반환한다")
    void shouldReturnNull_whenKeyDoesNotExist() {
      Integer rank = rankingRepository.getRank("non_existent_key", 1L);

      assertThat(rank).isNull();
    }
  }
}
