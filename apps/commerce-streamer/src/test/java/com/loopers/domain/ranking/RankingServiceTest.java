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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private RankingWeight rankingWeight;

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        rankingService = new RankingService(redisTemplate, rankingWeight);
    }

    @Nested
    @DisplayName("조회 점수 증가")
    class IncrementViewScore {

        @Test
        @DisplayName("조회 이벤트 발생 시 ZSET에 가중치 점수가 추가된다")
        void shouldIncrementScoreWithViewWeight() {
            // given
            Long productId = 100L;
            LocalDate date = LocalDate.of(2025, 1, 15);
            String expectedKey = "ranking:all:20250115";

            when(rankingWeight.calculateViewScore()).thenReturn(0.1);
            when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

            // when
            rankingService.incrementViewScore(productId, date);

            // then
            verify(zSetOperations).incrementScore(expectedKey, "100", 0.1);
        }

        @Test
        @DisplayName("새로운 키가 생성되면 TTL이 설정된다")
        void shouldSetTtlWhenKeyIsNew() {
            // given
            Long productId = 100L;
            LocalDate date = LocalDate.of(2025, 1, 15);
            String expectedKey = "ranking:all:20250115";

            when(rankingWeight.calculateViewScore()).thenReturn(0.1);
            when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

            // when
            rankingService.incrementViewScore(productId, date);

            // then
            verify(redisTemplate).expire(eq(expectedKey), any());
        }
    }

    @Nested
    @DisplayName("좋아요 점수 증가")
    class UpdateLikeScore {

        @Test
        @DisplayName("좋아요 이벤트 발생 시 양수 점수가 추가된다")
        void shouldIncrementScoreWhenLiked() {
            // given
            Long productId = 100L;
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(rankingWeight.calculateLikeScore(true)).thenReturn(0.2);
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // when
            rankingService.updateLikeScore(productId, true, date);

            // then
            verify(zSetOperations).incrementScore(anyString(), eq("100"), eq(0.2));
        }

        @Test
        @DisplayName("좋아요 취소 이벤트 발생 시 음수 점수가 추가된다")
        void shouldDecrementScoreWhenUnliked() {
            // given
            Long productId = 100L;
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(rankingWeight.calculateLikeScore(false)).thenReturn(-0.2);
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // when
            rankingService.updateLikeScore(productId, false, date);

            // then
            verify(zSetOperations).incrementScore(anyString(), eq("100"), eq(-0.2));
        }
    }

    @Nested
    @DisplayName("주문 점수 증가")
    class IncrementOrderScore {

        @Test
        @DisplayName("주문 수량 기반으로 점수가 계산된다")
        void shouldCalculateScoreBasedOnQuantity() {
            // given
            Long productId = 100L;
            int quantity = 5;
            LocalDate date = LocalDate.of(2025, 1, 15);

            when(rankingWeight.calculateOrderScore(quantity)).thenReturn(3.5); // 0.7 * 5
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // when
            rankingService.incrementOrderScore(productId, quantity, date);

            // then
            verify(zSetOperations).incrementScore(anyString(), eq("100"), eq(3.5));
        }
    }

    @Nested
    @DisplayName("전날 점수 다음날 이관")
    class CarryOverScores {

        @Test
        @DisplayName("전날 점수의 일부가 다음날 키로 복사된다")
        void shouldCopyScoresWithWeight() {
            // given
            LocalDate today = LocalDate.of(2025, 1, 15);
            LocalDate tomorrow = LocalDate.of(2025, 1, 16);
            String fromKey = "ranking:all:20250115";
            String toKey = "ranking:all:20250116";

            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            tuples.add(createTuple("100", 10.0));
            tuples.add(createTuple("200", 5.0));

            when(zSetOperations.zCard(toKey)).thenReturn(0L);
            when(zSetOperations.rangeWithScores(fromKey, 0, -1)).thenReturn(tuples);

            // when
            rankingService.carryOverScores(today, tomorrow, 0.1);

            // then
            verify(zSetOperations).add(toKey, "100", 1.0);  // 10.0 * 0.1
            verify(zSetOperations).add(toKey, "200", 0.5);  // 5.0 * 0.1
            verify(redisTemplate).expire(eq(toKey), any());
        }

        @Test
        @DisplayName("이미 준비된 랭킹이 있으면 스킵한다")
        void shouldSkipIfAlreadyPrepared() {
            // given
            LocalDate today = LocalDate.of(2025, 1, 15);
            LocalDate tomorrow = LocalDate.of(2025, 1, 16);
            String toKey = "ranking:all:20250116";

            when(zSetOperations.zCard(toKey)).thenReturn(10L); // 이미 데이터 존재

            // when
            rankingService.carryOverScores(today, tomorrow, 0.1);

            // then
            verify(zSetOperations, never()).rangeWithScores(anyString(), anyLong(), anyLong());
            verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
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
}
