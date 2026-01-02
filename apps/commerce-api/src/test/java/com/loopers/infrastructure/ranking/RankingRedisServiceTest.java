package com.loopers.infrastructure.ranking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RankingRedisServiceTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RankingRedisService rankingRedisService;

    private static final LocalDate TODAY = LocalDate.now();
    private static final String KEY = "ranking:all:" + TODAY.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    @BeforeEach
    void setUp() {
        redisTemplate.delete(KEY);
    }

    @Test
    @DisplayName("상위 N개 상품을 점수 높은 순으로 조회한다")
    void getTopProducts() {
        // arrange
        redisTemplate.opsForZSet().add(KEY, "1", 3.0);  // 상품1: 3점
        redisTemplate.opsForZSet().add(KEY, "2", 5.0);  // 상품2: 5점
        redisTemplate.opsForZSet().add(KEY, "3", 1.0);  // 상품3: 1점

        // act
        List<RankingEntry> result = rankingRedisService.getTopProducts(TODAY, 0, 3);

        // assert
        // 예상 순서: 상품2(5점) > 상품1(3점) > 상품3(1점)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).productId()).isEqualTo(2L);
        assertThat(result.get(0).score()).isEqualTo(5.0);
        assertThat(result.get(1).productId()).isEqualTo(1L);
        assertThat(result.get(2).productId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("상품 조회 시 1점을 증가시킨다")
    void incrementScoreForView() {
        // arrange - 아무 데이터 없는 상태

        // act
        rankingRedisService.incrementScoreForView(TODAY, 1L);

        // assert
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    @DisplayName("상품 조회 시 기존 점수에 1점을 누적한다")
    void incrementScoreForView_accumulates() {
        // arrange - 이미 3.0점 있는 상태
        redisTemplate.opsForZSet().add(KEY, "1", 3.0);

        // act
        rankingRedisService.incrementScoreForView(TODAY, 1L);

        // assert
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(4.0);
    }

    @Test
    @DisplayName("좋아요 시 2점을 증가시킨다")
    void incrementScoreForLike() {
        // act
        rankingRedisService.incrementScoreForLike(TODAY, 1L);

        // assert
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(2.0);
    }

    @Test
    @DisplayName("주문 시 수량 * 6점을 증가시킨다")
    void incrementScoreForOrder() {
        // act - 3개 주문
        rankingRedisService.incrementScoreForOrder(TODAY, 1L, 3L);

        // assert - 6.0 * 3 = 18.0
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(18.0);
    }

    @Test
    @DisplayName("점수 증가 시 TTL이 2일로 설정된다")
    void incrementScore_setsTTL() {
        // act
        rankingRedisService.incrementScoreForView(TODAY, 1L);

        // assert - TTL이 설정되어 있어야 함 (2일 = 172800초, 약간의 오차 허용)
        Long ttl = redisTemplate.getExpire(KEY);
        assertThat(ttl).isGreaterThan(172800 - 60);  // 최소 2일 - 1분
        assertThat(ttl).isLessThanOrEqualTo(172800); // 최대 2일
    }

    @Test
    @DisplayName("특정 상품의 순위를 조회한다 (1-based)")
    void getRankingPosition() {
        // arrange
        redisTemplate.opsForZSet().add(KEY, "1", 3.0);  // 상품1: 3점 -> 2위
        redisTemplate.opsForZSet().add(KEY, "2", 5.0);  // 상품2: 5점 -> 1위
        redisTemplate.opsForZSet().add(KEY, "3", 1.0);  // 상품3: 1점 -> 3위

        // act & assert
        assertThat(rankingRedisService.getRankingPosition(TODAY, 2L)).isEqualTo(1L);
        assertThat(rankingRedisService.getRankingPosition(TODAY, 1L)).isEqualTo(2L);
        assertThat(rankingRedisService.getRankingPosition(TODAY, 3L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("랭킹에 없는 상품은 null을 반환한다")
    void getRankingPosition_notFound() {
        // arrange - 상품 999는 랭킹에 없음

        // act
        Long position = rankingRedisService.getRankingPosition(TODAY, 999L);

        // assert
        assertThat(position).isNull();
    }

    @Test
    @DisplayName("랭킹에 등록된 전체 상품 수를 조회한다")
    void getTotalCount() {
        // arrange
        redisTemplate.opsForZSet().add(KEY, "1", 3.0);
        redisTemplate.opsForZSet().add(KEY, "2", 5.0);
        redisTemplate.opsForZSet().add(KEY, "3", 1.0);

        // act
        long count = rankingRedisService.getTotalCount(TODAY);

        // assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("랭킹이 비어있으면 0을 반환한다")
    void getTotalCount_empty() {
        // act
        long count = rankingRedisService.getTotalCount(TODAY);

        // assert
        assertThat(count).isEqualTo(0);
    }
}
