package com.loopers.infrastructure.consumer;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.ranking.RankingRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCompletedConsumerTest {

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Mock
    private ProductMetricsRepository productMetricsRepository;

    @Mock
    private RankingRedisService rankingRedisService;

    @Mock
    private Acknowledgment acknowledgment;

    private OrderCompletedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderCompletedConsumer(eventHandledRepository, productMetricsRepository, rankingRedisService);
    }

    @Test
    @DisplayName("주문 완료 이벤트 수신 시 주문한 상품들의 랭킹 점수를 올리고 ProductMetrics에 저장한다")
    void consumeTest1() {
        OrderItem item1 = OrderItem.create(1L, "상품1", 2L, 10000);
        OrderItem item2 = OrderItem.create(2L, "상품2", 3L, 20000);
        Order order = Order.create("user1", List.of(item1, item2), 80000);
        ReflectionTestUtils.setField(order, "id", 100L);

        OrderCompletedEvent event = OrderCompletedEvent.from(order);
        LocalDate eventDate = event.getOccurredAt().toLocalDate();

        when(eventHandledRepository.existsByEventId("300")).thenReturn(false);

        consumer.consume(event, "300", acknowledgment);

        verify(productMetricsRepository).upsertOrder(eq(1L), eq(eventDate), eq(2));
        verify(productMetricsRepository).upsertOrder(eq(2L), eq(eventDate), eq(3));
        verify(rankingRedisService).incrementScoreForOrder(eq(eventDate), eq(1L), eq(2L));
        verify(rankingRedisService).incrementScoreForOrder(eq(eventDate), eq(2L), eq(3L));
        verify(eventHandledRepository).save(argThat(e -> e.getEventId().equals("300")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 무시하고 ack만 한다")
    void consumeTest2() {
        OrderItem item = OrderItem.create(1L, "상품1", 1L, 10000);
        Order order = Order.create("user1", List.of(item), 10000);
        ReflectionTestUtils.setField(order, "id", 100L);

        OrderCompletedEvent event = OrderCompletedEvent.from(order);
        when(eventHandledRepository.existsByEventId("300")).thenReturn(true);

        consumer.consume(event, "300", acknowledgment);

        verify(productMetricsRepository, never()).save(any());
        verify(rankingRedisService, never()).incrementScoreForOrder(any(), any(), any(Long.class));
        verify(eventHandledRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }
}
