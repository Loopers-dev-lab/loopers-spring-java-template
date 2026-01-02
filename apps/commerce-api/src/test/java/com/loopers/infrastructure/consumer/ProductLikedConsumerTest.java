package com.loopers.infrastructure.consumer;

import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import com.loopers.infrastructure.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.ranking.RankingRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductLikedConsumerTest {

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Mock
    private ProductMetricsRepository productMetricsRepository;

    @Mock
    private RankingRedisService rankingRedisService;

    @Mock
    private Acknowledgment acknowledgment;

    private ProductLikedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ProductLikedConsumer(eventHandledRepository, productMetricsRepository, rankingRedisService);
    }

    @Test
    @DisplayName("좋아요 이벤트 수신 시 ProductMetrics의 likeCount를 증가시키고 랭킹 점수를 올린다")
    void consumeTest1() {
        ProductLikedEvent event = ProductLikedEvent.liked(1L, 100L);
        LocalDate eventDate = event.getOccurredAt().toLocalDate();

        ProductMetrics metrics = ProductMetrics.create(1L, eventDate);
        when(eventHandledRepository.existsByEventId("100")).thenReturn(false);
        when(productMetricsRepository.findByProductIdAndDateForUpdate(eq(1L), eq(eventDate))).thenReturn(Optional.of(metrics));

        consumer.consume(event, "100", acknowledgment);

        verify(productMetricsRepository).upsertLikeCount(eq(1L), eq(eventDate), eq(0));
        verify(productMetricsRepository).findByProductIdAndDateForUpdate(eq(1L), eq(eventDate));
        verify(eventHandledRepository).save(argThat(e -> e.getEventId().equals("100")));
        verify(productMetricsRepository).save(argThat(m -> m.getProductId().equals(1L) && m.getLikeCount() == 1L));
        verify(rankingRedisService).incrementScoreForLike(eq(eventDate), eq(1L));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("좋아요 취소 이벤트 수신 시 ProductMetrics의 likeCount를 감소시키지만 랭킹 점수는 변경하지 않는다")
    void consumeTest2() {
        ProductLikedEvent event = ProductLikedEvent.unliked(1L, 100L);
        LocalDate eventDate = event.getOccurredAt().toLocalDate();

        ProductMetrics existingMetrics = ProductMetrics.create(1L, eventDate);
        existingMetrics.updateLikeIfNewer(true, LocalDateTime.of(2024, 1, 1, 10, 0));
        existingMetrics.updateLikeIfNewer(true, LocalDateTime.of(2024, 1, 1, 11, 0));

        when(eventHandledRepository.existsByEventId("101")).thenReturn(false);
        when(productMetricsRepository.findByProductIdAndDateForUpdate(eq(1L), eq(eventDate))).thenReturn(Optional.of(existingMetrics));

        consumer.consume(event, "101", acknowledgment);

        verify(productMetricsRepository).upsertLikeCount(eq(1L), eq(eventDate), eq(0));
        verify(productMetricsRepository).save(argThat(m -> m.getLikeCount() == 1L));
        verify(rankingRedisService, never()).incrementScoreForLike(any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 무시하고 ack만 한다")
    void consumeTest3() {
        ProductLikedEvent event = ProductLikedEvent.liked(1L, 100L);
        when(eventHandledRepository.existsByEventId("100")).thenReturn(true);

        consumer.consume(event, "100", acknowledgment);

        verify(productMetricsRepository, never()).save(any());
        verify(eventHandledRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }
}
