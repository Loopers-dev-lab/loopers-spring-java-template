package com.loopers.application.metrics;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.infrastructure.cache.ProductRankingCache;
import com.loopers.infrastructure.cache.ProductRankingCache.OrderItemScore;
import com.loopers.infrastructure.persistence.EventHandledRepository;
import com.loopers.infrastructure.persistence.ProductMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 메트릭 집계 서비스 - 랭킹 적재 테스트
 */
@ExtendWith(MockitoExtension.class)
class MetricsAggregationServiceRankingTest {

    @Mock
    private ProductMetricsRepository metricsRepository;

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Mock
    private ProductRankingCache productRankingCache;

    @InjectMocks
    private MetricsAggregationService aggregationService;

    private static final Long PRODUCT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long BRAND_ID = 10L;

    private void setupNonDuplicateEvent() {
        when(eventHandledRepository.existsById(anyString())).thenReturn(false);
        when(metricsRepository.findById(PRODUCT_ID))
            .thenReturn(Optional.of(ProductMetrics.create(PRODUCT_ID)));
    }

    @Test
    @DisplayName("handleProductLiked - 랭킹 점수가 적재된다")
    void handleProductLiked_addsRankingScore() {
        // given
        setupNonDuplicateEvent();
        String eventId = "event-liked-001";
        ProductLikedEvent event = new ProductLikedEvent(
            MEMBER_ID, PRODUCT_ID, BRAND_ID, LocalDateTime.now()
        );

        // when
        aggregationService.handleProductLiked(eventId, event);

        // then
        verify(productRankingCache).addLikeScore(PRODUCT_ID);
    }

    @Test
    @DisplayName("handleProductUnliked - 랭킹 점수가 감소한다")
    void handleProductUnliked_subtractsRankingScore() {
        // given
        setupNonDuplicateEvent();
        String eventId = "event-unliked-001";
        ProductUnlikedEvent event = new ProductUnlikedEvent(
            MEMBER_ID, PRODUCT_ID, BRAND_ID, LocalDateTime.now()
        );

        // when
        aggregationService.handleProductUnliked(eventId, event);

        // then
        verify(productRankingCache).subtractLikeScore(PRODUCT_ID);
    }

    @Test
    @DisplayName("handleProductViewed - 랭킹 점수가 적재된다")
    void handleProductViewed_addsRankingScore() {
        // given
        setupNonDuplicateEvent();
        String eventId = "event-viewed-001";
        ProductViewedEvent event = new ProductViewedEvent(
            MEMBER_ID, PRODUCT_ID, BRAND_ID, LocalDateTime.now()
        );

        // when
        aggregationService.handleProductViewed(eventId, event);

        // then
        verify(productRankingCache).addViewScore(PRODUCT_ID);
    }

    @Test
    @DisplayName("handleOrderCompleted - 배치로 랭킹 점수가 적재된다")
    void handleOrderCompleted_addsBatchRankingScore() {
        // given
        when(eventHandledRepository.existsById(anyString())).thenReturn(false);
        String eventId = "event-order-001";
        Long productId1 = 100L;
        Long productId2 = 200L;

        when(metricsRepository.findById(productId1))
            .thenReturn(Optional.of(ProductMetrics.create(productId1)));
        when(metricsRepository.findById(productId2))
            .thenReturn(Optional.of(ProductMetrics.create(productId2)));

        OrderCompletedEvent event = new OrderCompletedEvent(
            "ORDER-001",
            MEMBER_ID,
            BigDecimal.valueOf(75000),
            List.of(
                new OrderCompletedEvent.OrderItemInfo(productId1, 2, BigDecimal.valueOf(25000)),
                new OrderCompletedEvent.OrderItemInfo(productId2, 1, BigDecimal.valueOf(25000))
            ),
            LocalDateTime.now()
        );

        // when
        aggregationService.handleOrderCompleted(eventId, event);

        // then
        ArgumentCaptor<List<OrderItemScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRankingCache).addOrderScoresBatch(captor.capture());

        List<OrderItemScore> capturedItems = captor.getValue();
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems.get(0).productId()).isEqualTo(productId1);
        assertThat(capturedItems.get(0).price()).isEqualTo(25000L);
        assertThat(capturedItems.get(0).quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("중복 이벤트는 랭킹 점수를 적재하지 않는다")
    void duplicateEvent_doesNotAddRankingScore() {
        // given
        String eventId = "event-liked-duplicate";
        when(eventHandledRepository.existsById(eventId)).thenReturn(true);

        ProductLikedEvent event = new ProductLikedEvent(
            MEMBER_ID, PRODUCT_ID, BRAND_ID, LocalDateTime.now()
        );

        // when
        aggregationService.handleProductLiked(eventId, event);

        // then
        verify(productRankingCache, never()).addLikeScore(anyLong());
    }
}
