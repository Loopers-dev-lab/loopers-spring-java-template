package com.loopers.domain.ranking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingWeightTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RankingWeight rankingWeight;

    private static final String WEIGHT_KEY = "ranking:config:weights";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        rankingWeight = new RankingWeight(redisTemplate);
    }

    @Nested
    @DisplayName("가중치 반환")
    class GetWeight {

        @Test
        @DisplayName("Redis에 값이 있으면 해당 값을 반환한다")
        void shouldReturnRedisValueWhenExists() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "view")).thenReturn("0.15");

            // when
            double weight = rankingWeight.getViewWeight();

            // then
            assertThat(weight).isEqualTo(0.15);
        }

        @Test
        @DisplayName("Redis에 값이 없으면 기본값을 반환한다")
        void shouldReturnDefaultWhenRedisValueIsNull() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "view")).thenReturn(null);

            // when
            double weight = rankingWeight.getViewWeight();

            // then
            assertThat(weight).isEqualTo(0.1); // DEFAULT_VIEW_WEIGHT
        }

        @Test
        @DisplayName("Redis 예외 발생 시 기본값을 반환한다")
        void shouldReturnDefaultWhenRedisThrowsException() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "like")).thenThrow(new RuntimeException("Redis error"));

            // when
            double weight = rankingWeight.getLikeWeight();

            // then
            assertThat(weight).isEqualTo(0.2); // DEFAULT_LIKE_WEIGHT
        }
    }

    @Nested
    @DisplayName("가중치 계산")
    class CalculateScore {

        @Test
        @DisplayName("조회 점수 계산: viewWeight * 1.0")
        void shouldCalculateViewScore() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "view")).thenReturn("0.1");

            // when
            double score = rankingWeight.calculateViewScore();

            // then
            assertThat(score).isEqualTo(0.1);
        }

        @Test
        @DisplayName("좋아요 점수 계산: likeWeight * 1.0 (좋아요)")
        void shouldCalculateLikeScoreWhenLiked() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "like")).thenReturn("0.2");

            // when
            double score = rankingWeight.calculateLikeScore(true);

            // then
            assertThat(score).isEqualTo(0.2);
        }

        @Test
        @DisplayName("좋아요 취소 점수 계산: likeWeight * -1.0 (취소)")
        void shouldCalculateNegativeScoreWhenUnliked() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "like")).thenReturn("0.2");

            // when
            double score = rankingWeight.calculateLikeScore(false);

            // then
            assertThat(score).isEqualTo(-0.2);
        }

        @Test
        @DisplayName("주문 점수 계산 (수량 기반): orderWeight * quantity")
        void shouldCalculateOrderScoreWithQuantity() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "order")).thenReturn("0.7");

            // when
            double score = rankingWeight.calculateOrderScore(5);

            // then
            assertThat(score).isEqualTo(3.5); // 0.7 * 5
        }

        @Test
        @DisplayName("주문 점수 계산 (금액 기반): orderWeight * log10(amount)")
        void shouldCalculateOrderScoreWithAmount() {
            // given
            when(hashOperations.get(WEIGHT_KEY, "order")).thenReturn("0.7");

            // when
            double score = rankingWeight.calculateOrderScoreWithAmount(10000L);

            // then
            // 0.7 * log10(10000) = 0.7 * 4 = 2.8
            assertThat(score).isCloseTo(2.8, within(0.01));
        }

        @Test
        @DisplayName("금액이 0 이하일 때 점수는 0이다")
        void shouldReturnZeroScoreWhenAmountIsZeroOrNegative() {
            // when
            double scoreZero = rankingWeight.calculateOrderScoreWithAmount(0);
            double scoreNegative = rankingWeight.calculateOrderScoreWithAmount(-100);

            // then
            assertThat(scoreZero).isEqualTo(0);
            assertThat(scoreNegative).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("가중치 업데이트")
    class UpdateWeights {

        @Test
        @DisplayName("모든 가중치를 일괄 업데이트한다")
        void shouldUpdateAllWeights() {
            // when
            rankingWeight.updateAllWeights(0.15, 0.25, 0.6);

            // then
            verify(hashOperations).put(WEIGHT_KEY, "view", "0.15");
            verify(hashOperations).put(WEIGHT_KEY, "like", "0.25");
            verify(hashOperations).put(WEIGHT_KEY, "order", "0.6");
        }

        @Test
        @DisplayName("가중치 초기화 시 Redis 키를 삭제한다")
        void shouldDeleteKeyWhenReset() {
            // when
            rankingWeight.resetToDefault();

            // then
            verify(redisTemplate).delete(WEIGHT_KEY);
        }
    }
}
