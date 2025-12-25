package com.loopers.application.like;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductLikeEventHandlerTest {

    @Autowired
    private ProductLikeEventHandler productLikeEventHandler;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private EventHandledRepository eventHandledRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("중복 메시지 재전송 시 한번만 처리된다")
    void whenDuplicateMessagesAreReceived_thenShouldProcessOnlyOnce() {
        // Given
        String eventId = "test-event-id-123";
        Long productId = 1L;

        // When: 첫 번째 처리
        productLikeEventHandler.handleLikeAdded(eventId, productId);

        // Then: 좋아요 수가 1 증가
        ProductMetrics metrics1 = productMetricsRepository.findByProductId(productId).orElseThrow();
        assertThat(metrics1.getLikeCount()).isEqualTo(1L);

        // When: 동일한 eventId로 두 번째 처리 시도
        productLikeEventHandler.handleLikeAdded(eventId, productId);

        // Then: 좋아요 수가 그대로 1 (중복 처리 방지)
        ProductMetrics metrics2 = productMetricsRepository.findByProductId(productId).orElseThrow();
        assertThat(metrics2.getLikeCount()).isEqualTo(1L);

        // Then: event_handled에 한 번만 기록됨
        List<EventHandled> events = eventHandledRepository.findByEventId(eventId);
        assertThat(events).hasSize(1);
    }
}
