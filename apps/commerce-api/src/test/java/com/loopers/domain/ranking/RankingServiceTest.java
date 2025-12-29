package com.loopers.domain.ranking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        rankingService = new RankingService(redisTemplate);
    }

    @Nested
    @DisplayName("getTopNWithScores")
    class GetTopNWithScores {

        @Test
        @DisplayName("Top-N 랭킹을 점수와 함께 조회한다")
        void shouldReturnTopNWithScores() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);
            String key = "ranking:all:20250115";

            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            tuples.add(createTuple("100", 10.5));
            tuples.add(createTuple("200", 8.3));
            tuples.add(createTuple("300", 5.1));

            when(zSetOperations.reverseRangeWithScores(key, 0, 2)).thenReturn(tuples);

            // when
            List<RankingEntry> entries = rankingService.getTopNWithScores(date, 3);

            // then
            assertThat(entries).hasSize(3);
            assertThat(entries.get(0).productId()).isEqualTo(100L);
            assertThat(entries.get(0).score()).isEqualTo(10.5);
            assertThat(entries.get(1).productId()).isEqualTo(200L);
            assertThat(entries.get(2).productId()).isEqualTo(300L);
        }

        @Test
        @DisplayName("데이터가 없으면 빈 리스트를 반환한다")
        void shouldReturnEmptyListWhenNoData() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                    .thenReturn(null);

            // when
            List<RankingEntry> entries = rankingService.getTopNWithScores(date, 10);

            // then
            assertThat(entries).isEmpty();
        }

        @Test
        @DisplayName("Redis 예외 발생 시 빈 리스트를 반환한다")
        void shouldReturnEmptyListOnException() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                    .thenThrow(new RuntimeException("Redis error"));

            // when
            List<RankingEntry> entries = rankingService.getTopNWithScores(date, 10);

            // then
            assertThat(entries).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRankingPage")
    class GetRankingPage {

        @Test
        @DisplayName("페이지네이션으로 랭킹을 조회한다")
        void shouldReturnPaginatedRanking() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);
            String key = "ranking:all:20250115";
            int page = 1;
            int size = 10;

            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            tuples.add(createTuple("110", 4.5));
            tuples.add(createTuple("120", 4.2));

            when(zSetOperations.reverseRangeWithScores(key, 10, 19)).thenReturn(tuples);

            // when
            List<RankingEntry> entries = rankingService.getRankingPage(date, page, size);

            // then
            assertThat(entries).hasSize(2);
            verify(zSetOperations).reverseRangeWithScores(key, 10, 19);
        }
    }

    @Nested
    @DisplayName("getRank")
    class GetRank {

        @Test
        @DisplayName("상품의 순위를 조회한다 (1-based)")
        void shouldReturnRankOneBased() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);
            Long productId = 100L;

            when(zSetOperations.reverseRank(anyString(), eq("100"))).thenReturn(0L);

            // when
            Long rank = rankingService.getRank(productId, date);

            // then
            assertThat(rank).isEqualTo(1L); // 0 -> 1 (1-based)
        }

        @Test
        @DisplayName("랭킹에 없는 상품은 null을 반환한다")
        void shouldReturnNullWhenNotInRanking() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);
            Long productId = 999L;

            when(zSetOperations.reverseRank(anyString(), eq("999"))).thenReturn(null);

            // when
            Long rank = rankingService.getRank(productId, date);

            // then
            assertThat(rank).isNull();
        }
    }

    @Nested
    @DisplayName("getScore")
    class GetScore {

        @Test
        @DisplayName("상품의 점수를 조회한다")
        void shouldReturnScore() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);
            Long productId = 100L;

            when(zSetOperations.score(anyString(), eq("100"))).thenReturn(15.5);

            // when
            Double score = rankingService.getScore(productId, date);

            // then
            assertThat(score).isEqualTo(15.5);
        }
    }

    @Nested
    @DisplayName("getRankingSize")
    class GetRankingSize {

        @Test
        @DisplayName("랭킹에 진입한 상품 수를 조회한다")
        void shouldReturnRankingSize() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(zSetOperations.zCard(anyString())).thenReturn(150L);

            // when
            Long size = rankingService.getRankingSize(date);

            // then
            assertThat(size).isEqualTo(150L);
        }

        @Test
        @DisplayName("키가 없으면 0을 반환한다")
        void shouldReturnZeroWhenKeyNotExists() {
            // given
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(zSetOperations.zCard(anyString())).thenReturn(null);

            // when
            Long size = rankingService.getRankingSize(date);

            // then
            assertThat(size).isEqualTo(0L);
        }
    }

    private ZSetOperations.TypedTuple<String> createTuple(String value, Double score) {
        return new ZSetOperations.TypedTuple<>() {
            @Override
            public String getValue() { return value; }
            @Override
            public Double getScore() { return score; }
            @Override
            public int compareTo(ZSetOperations.TypedTuple<String> o) {
                return Double.compare(this.getScore(), o.getScore());
            }
        };
    }
}
