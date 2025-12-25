package com.loopers.application.product;

import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductEventHandlerTest {

    @Autowired
    private ProductEventHandler productEventHandler;

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
    @DisplayName("중복 상품 조회 이벤트 재전송 시 한번만 처리된다")
    void whenDuplicateMessagesAreReceived_thenShouldProcessOnlyOnce() {
        // Given
        String eventId = "test-event-id-456";
        Long productId = 2L;

        // When: 첫 번째 처리
        productEventHandler.handleProductViewed(eventId, productId);

        // When: 동일한 eventId로 두 번째 처리
        productEventHandler.handleProductViewed(eventId, productId);

        // Then: 조회 수가 1 (중복 처리 방지)
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseThrow();
        assertThat(metrics.getViewCount()).isEqualTo(1L);
    }
}
