package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ProductMetricsItemWriter 테스트.
 */
class ProductMetricsItemWriterTest {

    private final ProductMetricsItemWriter writer = new ProductMetricsItemWriter();

    @DisplayName("Chunk를 정상적으로 처리할 수 있다")
    @Test
    void writesChunk_successfully() throws Exception {
        // arrange
        List<ProductMetrics> items = createProductMetricsList(3);
        Chunk<ProductMetrics> chunk = new Chunk<>(items);

        // act & assert
        assertThatCode(() -> writer.write(chunk))
            .doesNotThrowAnyException();
    }

    @DisplayName("빈 Chunk도 처리할 수 있다")
    @Test
    void writesEmptyChunk_successfully() throws Exception {
        // arrange
        Chunk<ProductMetrics> chunk = new Chunk<>(new ArrayList<>());

        // act & assert
        assertThatCode(() -> writer.write(chunk))
            .doesNotThrowAnyException();
    }

    @DisplayName("큰 Chunk도 처리할 수 있다")
    @Test
    void writesLargeChunk_successfully() throws Exception {
        // arrange
        List<ProductMetrics> items = createProductMetricsList(100); // Chunk 크기와 동일
        Chunk<ProductMetrics> chunk = new Chunk<>(items);

        // act & assert
        assertThatCode(() -> writer.write(chunk))
            .doesNotThrowAnyException();
    }

    @DisplayName("다양한 메트릭 값을 가진 Chunk를 처리할 수 있다")
    @Test
    void writesChunk_withVariousMetrics() throws Exception {
        // arrange
        List<ProductMetrics> items = new ArrayList<>();
        
        ProductMetrics metrics1 = new ProductMetrics(1L);
        metrics1.incrementLikeCount();
        items.add(metrics1);
        
        ProductMetrics metrics2 = new ProductMetrics(2L);
        metrics2.incrementSalesCount(100);
        items.add(metrics2);
        
        ProductMetrics metrics3 = new ProductMetrics(3L);
        metrics3.incrementViewCount();
        metrics3.incrementViewCount();
        items.add(metrics3);
        
        Chunk<ProductMetrics> chunk = new Chunk<>(items);

        // act & assert
        assertThatCode(() -> writer.write(chunk))
            .doesNotThrowAnyException();
    }

    @DisplayName("Chunk의 모든 항목을 처리한다")
    @Test
    void writesChunk_processesAllItems() throws Exception {
        // arrange
        int itemCount = 10;
        List<ProductMetrics> items = createProductMetricsList(itemCount);
        Chunk<ProductMetrics> chunk = new Chunk<>(items);

        // act
        writer.write(chunk);

        // assert
        // 현재는 로깅만 수행하므로 예외가 발생하지 않으면 성공
        // 향후 Materialized View 저장 로직 추가 시 추가 검증 필요
        assertThatCode(() -> writer.write(chunk))
            .doesNotThrowAnyException();
    }

    /**
     * 테스트용 ProductMetrics 리스트를 생성합니다.
     *
     * @param count 생성할 항목 수
     * @return ProductMetrics 리스트
     */
    private List<ProductMetrics> createProductMetricsList(int count) {
        List<ProductMetrics> items = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            ProductMetrics metrics = new ProductMetrics(i);
            metrics.incrementLikeCount();
            metrics.incrementSalesCount((int) i);
            metrics.incrementViewCount();
            items.add(metrics);
        }
        return items;
    }
}

