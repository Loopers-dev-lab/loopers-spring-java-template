package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductMetricsItemProcessor 테스트.
 */
class ProductMetricsItemProcessorTest {

    private final ProductMetricsItemProcessor processor = new ProductMetricsItemProcessor();

    @DisplayName("ProductMetrics를 그대로 전달한다 (pass-through)")
    @Test
    void processesItem_andReturnsSameItem() throws Exception {
        // arrange
        ProductMetrics item = new ProductMetrics(1L);
        item.incrementLikeCount();
        item.incrementSalesCount(10);
        item.incrementViewCount();

        // act
        ProductMetrics result = processor.process(item);

        // assert
        assertThat(result).isSameAs(item); // 동일한 객체 반환
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getLikeCount()).isEqualTo(1L);
        assertThat(result.getSalesCount()).isEqualTo(10L);
        assertThat(result.getViewCount()).isEqualTo(1L);
    }

    @DisplayName("null이 아닌 모든 ProductMetrics를 처리한다")
    @Test
    void processesNonNullItem() throws Exception {
        // arrange
        ProductMetrics item = new ProductMetrics(100L);

        // act
        ProductMetrics result = processor.process(item);

        // assert
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(item);
    }

    @DisplayName("여러 번 처리해도 동일한 결과를 반환한다")
    @Test
    void processesItemMultipleTimes_returnsSameResult() throws Exception {
        // arrange
        ProductMetrics item = new ProductMetrics(1L);
        item.incrementLikeCount();

        // act
        ProductMetrics result1 = processor.process(item);
        ProductMetrics result2 = processor.process(item);
        ProductMetrics result3 = processor.process(item);

        // assert
        assertThat(result1).isSameAs(item);
        assertThat(result2).isSameAs(item);
        assertThat(result3).isSameAs(item);
    }

    @DisplayName("초기값을 가진 ProductMetrics도 처리한다")
    @Test
    void processesItemWithInitialValues() throws Exception {
        // arrange
        ProductMetrics item = new ProductMetrics(1L);
        // 초기값: 모든 카운트가 0

        // act
        ProductMetrics result = processor.process(item);

        // assert
        assertThat(result).isSameAs(item);
        assertThat(result.getLikeCount()).isEqualTo(0L);
        assertThat(result.getSalesCount()).isEqualTo(0L);
        assertThat(result.getViewCount()).isEqualTo(0L);
    }
}

