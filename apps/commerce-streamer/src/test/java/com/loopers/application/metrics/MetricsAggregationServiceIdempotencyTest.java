package com.loopers.application.metrics;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.config.TestConfig;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.infrastructure.persistence.EventHandledRepository;
import com.loopers.infrastructure.persistence.ProductMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메트릭 집계 서비스 멱등성 테스트
 * - 중복 이벤트 처리 방지 검증
 * - event_handled 테이블 기반 멱등성 보장
 */
@DataJpaTest
@Import({MetricsAggregationService.class})
@ActiveProfiles("test")
class MetricsAggregationServiceIdempotencyTest {

    @Autowired
    private MetricsAggregationService aggregationService;

    @Autowired
    private ProductMetricsRepository metricsRepository;

    @Autowired
    private EventHandledRepository eventHandledRepository;

    private static final Long PRODUCT_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final Long BRAND_ID = 10L;

    @BeforeEach
    void setUp() {
        // 테스트용 ProductMetrics 초기화
        ProductMetrics metrics = ProductMetrics.create(PRODUCT_ID);
        metricsRepository.save(metrics);
    }

    @Test
    @DisplayName("좋아요 이벤트 중복 처리 방지 - 동일한 eventId로 두 번 호출 시 한 번만 처리됨")
    void shouldIgnoreDuplicateProductLikedEvent() {
        // given
        String eventId = "event-liked-001";
        ProductLikedEvent event = new ProductLikedEvent(
            MEMBER_ID,
            PRODUCT_ID,
            BRAND_ID,
            LocalDateTime.now()
        );

        // when - 첫 번째 이벤트 처리
        aggregationService.handleProductLiked(eventId, event);

        // then - 좋아요 수가 1로 증가
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(1);
        assertThat(eventHandledRepository.existsById(eventId)).isTrue();

        // when - 동일한 eventId로 두 번째 이벤트 처리 (중복)
        aggregationService.handleProductLiked(eventId, event);

        // then - 좋아요 수가 여전히 1 (증가하지 않음)
        metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 취소 이벤트 중복 처리 방지")
    void shouldIgnoreDuplicateProductUnlikedEvent() {
        // given - 좋아요 수를 먼저 1로 설정
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        metrics.incrementLikeCount();
        metricsRepository.save(metrics);

        String eventId = "event-unliked-001";
        ProductUnlikedEvent event = new ProductUnlikedEvent(
            MEMBER_ID,
            PRODUCT_ID,
            BRAND_ID,
            LocalDateTime.now()
        );

        // when - 첫 번째 이벤트 처리
        aggregationService.handleProductUnliked(eventId, event);

        // then - 좋아요 수가 0으로 감소
        metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(0);
        assertThat(eventHandledRepository.existsById(eventId)).isTrue();

        // when - 동일한 eventId로 두 번째 이벤트 처리 (중복)
        aggregationService.handleProductUnliked(eventId, event);

        // then - 좋아요 수가 여전히 0 (감소하지 않음)
        metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("주문 완료 이벤트 중복 처리 방지")
    void shouldIgnoreDuplicateOrderCompletedEvent() {
        // given
        String eventId = "event-order-001";
        OrderCompletedEvent event = new OrderCompletedEvent(
            "ORDER-001",
            MEMBER_ID,
            BigDecimal.valueOf(50000),
            List.of(
                new OrderCompletedEvent.OrderItemInfo(
                    PRODUCT_ID,
                    2,
                    BigDecimal.valueOf(25000)
                )
            ),
            LocalDateTime.now()
        );

        // when - 첫 번째 이벤트 처리
        aggregationService.handleOrderCompleted(eventId, event);

        // then - 판매량 증가
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getSalesCount()).isEqualTo(2);
        assertThat(metrics.getSalesAmount()).isEqualTo(50000);
        assertThat(eventHandledRepository.existsById(eventId)).isTrue();

        // when - 동일한 eventId로 두 번째 이벤트 처리 (중복)
        aggregationService.handleOrderCompleted(eventId, event);

        // then - 판매량이 여전히 2 (증가하지 않음)
        metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getSalesCount()).isEqualTo(2);
        assertThat(metrics.getSalesAmount()).isEqualTo(50000);
    }

    @Test
    @DisplayName("서로 다른 eventId는 각각 처리됨")
    void shouldProcessDifferentEventIds() {
        // given
        String eventId1 = "event-liked-001";
        String eventId2 = "event-liked-002";
        ProductLikedEvent event1 = new ProductLikedEvent(
            MEMBER_ID,
            PRODUCT_ID,
            BRAND_ID,
            LocalDateTime.now()
        );
        ProductLikedEvent event2 = new ProductLikedEvent(
            MEMBER_ID + 1,
            PRODUCT_ID,
            BRAND_ID,
            LocalDateTime.now()
        );

        // when - 서로 다른 eventId로 두 번 호출
        aggregationService.handleProductLiked(eventId1, event1);
        aggregationService.handleProductLiked(eventId2, event2);

        // then - 좋아요 수가 2로 증가
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(2);
        assertThat(eventHandledRepository.existsById(eventId1)).isTrue();
        assertThat(eventHandledRepository.existsById(eventId2)).isTrue();
    }

    @Test
    @DisplayName("상품 조회 이벤트 중복 처리 방지 - 동일한 eventId로 두 번 호출 시 한 번만 처리됨")
    void shouldIgnoreDuplicateProductViewedEvent() {
        // given
        String eventId = "event-viewed-001";
        ProductViewedEvent event = new ProductViewedEvent(
            MEMBER_ID,
            PRODUCT_ID,
            BRAND_ID,
            LocalDateTime.now()
        );

        // when - 첫 번째 이벤트 처리
        aggregationService.handleProductViewed(eventId, event);

        // then - 조회 수가 1로 증가
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getViewCount()).isEqualTo(1);
        assertThat(eventHandledRepository.existsById(eventId)).isTrue();

        // when - 동일한 eventId로 두 번째 이벤트 처리 (중복)
        aggregationService.handleProductViewed(eventId, event);

        // then - 조회 수가 여전히 1 (증가하지 않음)
        metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("비로그인 사용자 조회도 집계됨")
    void shouldAggregateViewFromAnonymousUser() {
        // given
        String eventId = "event-viewed-002";
        ProductViewedEvent event = new ProductViewedEvent(
            null,  // 비로그인 사용자
            PRODUCT_ID,
            BRAND_ID,
            LocalDateTime.now()
        );

        // when
        aggregationService.handleProductViewed(eventId, event);

        // then
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 조회 이벤트가 누적됨")
    void shouldAccumulateMultipleViewEvents() {
        // given
        String eventId1 = "event-viewed-003";
        String eventId2 = "event-viewed-004";
        String eventId3 = "event-viewed-005";

        ProductViewedEvent event1 = new ProductViewedEvent(MEMBER_ID, PRODUCT_ID, BRAND_ID, LocalDateTime.now());
        ProductViewedEvent event2 = new ProductViewedEvent(MEMBER_ID + 1, PRODUCT_ID, BRAND_ID, LocalDateTime.now());
        ProductViewedEvent event3 = new ProductViewedEvent(null, PRODUCT_ID, BRAND_ID, LocalDateTime.now());

        // when
        aggregationService.handleProductViewed(eventId1, event1);
        aggregationService.handleProductViewed(eventId2, event2);
        aggregationService.handleProductViewed(eventId3, event3);

        // then
        ProductMetrics metrics = metricsRepository.findById(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getViewCount()).isEqualTo(3);
    }
}
