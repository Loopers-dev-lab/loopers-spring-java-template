package com.loopers.application.consumer;

import com.loopers.interfaces.consumer.RankingConsumer;
import com.loopers.messaging.event.KafkaEventMessage;
import com.loopers.messaging.event.OrderPaidEvent;
import com.loopers.support.event.OrderPaidPayload;
import com.loopers.testcontainers.RedisTestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Import(RedisTestContainersConfig.class)
class RankingConsumerTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RankingConsumer rankingConsumer;

    private final LocalDate today = LocalDate.now();

    private static final long DAY = 60 * 60 * 24 * 1L;

    private static final double RANKING_VIEW_WEIGHT = 0.1;
    private static final double RANKING_LIKE_WEIGHT = 0.2;
    private static final double RANKING_ORDER_WEIGHT = 0.7;

    private static final Duration RANKING_TTL = Duration.ofDays(2);


    private String key() {
        return "ranking:all:" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(key());
    }

    @Test
    @DisplayName("조회 이벤트 소비 시 가중치에 따른 ZSET 점수가 적절히 반영된다")
    void rankingScoreUpdatedByViewEvents() {

        // arrange
        Long productId = 100L;
        List<OrderPaidEvent.OrderItemData> items = List.of(new OrderPaidEvent.OrderItemData(100L, 1, BigDecimal.valueOf(10000)));

        KafkaEventMessage<Object> message =
                new KafkaEventMessage<>(
                        UUID.randomUUID(),
                        "PRODUCT_VIEWED",
                        "PRODUCT",
                        productId,
                        items,
                        Instant.now()
                );

        // act
        rankingConsumer.handle(message);

        // assert
        Double score = redisTemplate.opsForZSet().score(key(), productId.toString());

        assertThat(score).isNotNull();
        assertThat(score).isEqualTo(RANKING_VIEW_WEIGHT);
    }

    @Test
    @DisplayName("주문 이벤트 소비 시 가중치에 따른 ZSET 점수가 적절히 반영된다")
    void rankingScoreUpdatedByOrderEvents() {

        // arrange
        Long orderId = 100L;

        KafkaEventMessage<Object> message =
                new KafkaEventMessage<>(
                        UUID.randomUUID(),
                        "ORDER_PAID",
                        "ORDER",
                        orderId,
                        new OrderPaidPayload(
                                List.of(
                                        new OrderPaidPayload.OrderItem(1L, 2, BigDecimal.valueOf(1000), false),
                                        new OrderPaidPayload.OrderItem(2L, 1, BigDecimal.valueOf(10000), false)
                                )
                        ),
                        Instant.now()
                );

        // act
        rankingConsumer.handle(message);

        // assert
        Double score1 = redisTemplate.opsForZSet().score(key(), "1");
        Double score2 = redisTemplate.opsForZSet().score(key(), "2");

        assertThat(score1).isNotNull();
        assertThat(score2).isNotNull();
        assertThat(score1).isEqualTo(RANKING_ORDER_WEIGHT * Math.log1p(1000));
        assertThat(score2).isEqualTo(RANKING_ORDER_WEIGHT * Math.log1p(10000));
    }

    @Test
    @DisplayName("좋아요 이벤트 소비 시 가중치에 따른 ZSET 점수가 적절히 반영된다")
    void rankingScoreUpdatedByLikeEvents() {

        // arrange
        Long productId = 100L;
        List<OrderPaidEvent.OrderItemData> items = List.of(new OrderPaidEvent.OrderItemData(100L, 1, BigDecimal.valueOf(10000)));

        KafkaEventMessage<Object> message =
                new KafkaEventMessage<>(
                        UUID.randomUUID(),
                        "LIKE_CREATED",
                        "PRODUCT",
                        productId,
                        null,
                        Instant.now()
                );

        // act
        rankingConsumer.handle(message);

        // assert
        Double score = redisTemplate.opsForZSet().score(key(), productId.toString());

        assertThat(score).isNotNull();
        assertThat(score).isEqualTo(RANKING_LIKE_WEIGHT);
    }

    @Test
    @DisplayName("ZSET 키 TTL 이 2일로 설정된다")
    void rankingKeyTtlIsTwoDays() {

        // arrange
        Long productId = 1L;
        KafkaEventMessage<Object> message = KafkaEventMessage.of(
                UUID.randomUUID(),
                "LIKE_CREATED",
                "PRODUCT",
                1L,
                null);

        rankingConsumer.handle(message);

        // act
        Long ttlSeconds = redisTemplate.getExpire(key());

        // assert
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isBetween(DAY, DAY * 2 + 10);
    }
}
