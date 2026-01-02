package com.loopers.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRankingCacheTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private ProductRankingCache productRankingCache;

    private static final Long PRODUCT_ID = 100L;
    private static final String TODAY_DATE = "20251226";

    @BeforeEach
    void setUp() {
        productRankingCache = new ProductRankingCache(redisTemplate);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("getRank - 순위가 1-based로 반환된다")
    void getRank_returnsOneBasedRank() {
        // given
        when(zSetOperations.reverseRank(anyString(), eq(PRODUCT_ID.toString())))
            .thenReturn(0L);  // 0-based rank

        // when
        Integer rank = productRankingCache.getRank(PRODUCT_ID, TODAY_DATE);

        // then
        assertThat(rank).isEqualTo(1);  // 1-based
    }

    @Test
    @DisplayName("getRank - 순위권 밖이면 null 반환")
    void getRank_returnsNullWhenNotRanked() {
        // given
        when(zSetOperations.reverseRank(anyString(), eq(PRODUCT_ID.toString())))
            .thenReturn(null);

        // when
        Integer rank = productRankingCache.getRank(PRODUCT_ID, TODAY_DATE);

        // then
        assertThat(rank).isNull();
    }

    @Test
    @DisplayName("getScore - 점수가 정상 반환된다")
    void getScore_returnsScore() {
        // given
        when(zSetOperations.score(anyString(), eq(PRODUCT_ID.toString())))
            .thenReturn(15.5);

        // when
        Double score = productRankingCache.getScore(PRODUCT_ID, TODAY_DATE);

        // then
        assertThat(score).isEqualTo(15.5);
    }

    @Test
    @DisplayName("getTopRankings - 페이지네이션이 동작한다")
    void getTopRankings_paginationWorks() {
        // given
        int page = 1;
        int size = 10;
        long expectedStart = 10L;  // page * size
        long expectedEnd = 19L;    // start + size - 1

        Set<ZSetOperations.TypedTuple<Object>> mockResults = new LinkedHashSet<>();
        mockResults.add(createTuple("101", 50.0));
        mockResults.add(createTuple("102", 45.0));

        when(zSetOperations.reverseRangeWithScores(anyString(), eq(expectedStart), eq(expectedEnd)))
            .thenReturn(mockResults);

        // when
        List<ProductRankingCache.RankingEntry> rankings =
            productRankingCache.getTopRankings(TODAY_DATE, page, size);

        // then
        assertThat(rankings).hasSize(2);
        assertThat(rankings.get(0).rank()).isEqualTo(11);  // page * size + 1
        assertThat(rankings.get(0).productId()).isEqualTo(101L);
        assertThat(rankings.get(0).score()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getTopRankings - 빈 결과 시 빈 리스트 반환")
    void getTopRankings_returnsEmptyListWhenNoResults() {
        // given
        when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
            .thenReturn(Collections.emptySet());

        // when
        List<ProductRankingCache.RankingEntry> rankings =
            productRankingCache.getTopRankings(TODAY_DATE, 0, 10);

        // then
        assertThat(rankings).isEmpty();
    }

    @Test
    @DisplayName("getTotalCount - 전체 개수를 반환한다")
    void getTotalCount_returnsTotalCount() {
        // given
        when(zSetOperations.zCard(anyString())).thenReturn(150L);

        // when
        long count = productRankingCache.getTotalCount(TODAY_DATE);

        // then
        assertThat(count).isEqualTo(150);
    }

    @Test
    @DisplayName("getTotalCount - null일 경우 0 반환")
    void getTotalCount_returnsZeroWhenNull() {
        // given
        when(zSetOperations.zCard(anyString())).thenReturn(null);

        // when
        long count = productRankingCache.getTotalCount(TODAY_DATE);

        // then
        assertThat(count).isEqualTo(0);
    }

    private ZSetOperations.TypedTuple<Object> createTuple(String value, Double score) {
        return new ZSetOperations.TypedTuple<>() {
            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public Double getScore() {
                return score;
            }

            @Override
            public int compareTo(ZSetOperations.TypedTuple<Object> o) {
                return Double.compare(score, o.getScore());
            }
        };
    }
}
