package com.loopers.infrastructure.cache;

import com.loopers.config.RankingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRankingCacheTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private RankingConfig rankingConfig;
    private ProductRankingCache productRankingCache;

    private static final Long PRODUCT_ID = 100L;

    @BeforeEach
    void setUp() {
        rankingConfig = new RankingConfig();
        rankingConfig.setTtlDays(2);
        RankingConfig.Weight weight = new RankingConfig.Weight();
        weight.setView(0.1);
        weight.setLike(0.2);
        weight.setOrder(0.6);
        rankingConfig.setWeight(weight);

        productRankingCache = new ProductRankingCache(redisTemplate, rankingConfig);
    }

    private void setupZSetOperations() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("addViewScore - 조회 점수가 가중치 0.1로 증가한다")
    void addViewScore_incrementsScoreWithWeight() {
        // given
        setupZSetOperations();

        // when
        productRankingCache.addViewScore(PRODUCT_ID);

        // then
        verify(zSetOperations).incrementScore(anyString(), eq(PRODUCT_ID.toString()), eq(0.1));
    }

    @Test
    @DisplayName("addLikeScore - 좋아요 점수가 가중치 0.2로 증가한다")
    void addLikeScore_incrementsScoreWithWeight() {
        // given
        setupZSetOperations();

        // when
        productRankingCache.addLikeScore(PRODUCT_ID);

        // then
        verify(zSetOperations).incrementScore(anyString(), eq(PRODUCT_ID.toString()), eq(0.2));
    }

    @Test
    @DisplayName("subtractLikeScore - 좋아요 취소 시 점수가 감소한다")
    void subtractLikeScore_decrementsScore() {
        // given
        setupZSetOperations();

        // when
        productRankingCache.subtractLikeScore(PRODUCT_ID);

        // then
        verify(zSetOperations).incrementScore(anyString(), eq(PRODUCT_ID.toString()), eq(-0.2));
    }

    @Test
    @DisplayName("addOrderScore - 주문 점수에 log 정규화가 적용된다")
    void addOrderScore_appliesLogNormalization() {
        // given
        setupZSetOperations();
        long price = 100000L;
        int quantity = 2;
        double expectedScore = 0.6 * Math.log1p(price * quantity);

        // when
        productRankingCache.addOrderScore(PRODUCT_ID, price, quantity);

        // then
        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOperations).incrementScore(anyString(), eq(PRODUCT_ID.toString()), scoreCaptor.capture());

        assertThat(scoreCaptor.getValue()).isCloseTo(expectedScore, within(0.0001));
    }

    @Test
    @DisplayName("addOrderScoresBatch - 여러 주문 아이템이 일괄 처리된다")
    void addOrderScoresBatch_processesBatch() {
        // given
        List<ProductRankingCache.OrderItemScore> orderItems = List.of(
            new ProductRankingCache.OrderItemScore(1L, 10000L, 1),
            new ProductRankingCache.OrderItemScore(2L, 20000L, 2),
            new ProductRankingCache.OrderItemScore(3L, 30000L, 3)
        );

        when(redisTemplate.executePipelined(any(SessionCallback.class))).thenReturn(List.of());

        // when
        productRankingCache.addOrderScoresBatch(orderItems);

        // then
        verify(redisTemplate).executePipelined(any(SessionCallback.class));
    }

    @Test
    @DisplayName("addOrderScoresBatch - 빈 리스트는 처리하지 않는다")
    void addOrderScoresBatch_skipsEmptyList() {
        // when
        productRankingCache.addOrderScoresBatch(List.of());

        // then
        verify(redisTemplate, never()).executePipelined(any(SessionCallback.class));
    }

    @Test
    @DisplayName("TTL이 첫 호출 시에만 설정된다")
    void ttl_setOnlyOnFirstCall() {
        // given
        setupZSetOperations();

        // when - 같은 날짜에 여러 번 호출
        productRankingCache.addViewScore(PRODUCT_ID);
        productRankingCache.addLikeScore(PRODUCT_ID);
        productRankingCache.addViewScore(PRODUCT_ID);

        // then - expire는 한 번만 호출됨
        verify(redisTemplate, times(1)).expire(anyString(), eq(2L), eq(TimeUnit.DAYS));
    }

    private static org.assertj.core.data.Offset<Double> within(double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}
