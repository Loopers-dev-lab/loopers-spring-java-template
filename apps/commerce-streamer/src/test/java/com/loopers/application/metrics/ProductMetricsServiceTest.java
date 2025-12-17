package com.loopers.application.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.testcontainers.RedisTestContainersConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(RedisTestContainersConfig.class)
class ProductMetricsServiceTest {

    @Autowired
    private ProductMetricsService productMetricsService;

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @BeforeEach
    void setUp() {
        productMetricsRepository.deleteAll();
    }

    @Test
    void 좋아요_수를_증가시킨다() {
        // Given
        Long productId = 1L;

        // When
        productMetricsService.incrementLikeCount(productId);

        // Then
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(1);
    }

    @Test
    void 좋아요_수를_감소시킨다() {
        // Given
        Long productId = 2L;
        productMetricsService.incrementLikeCount(productId);
        productMetricsService.incrementLikeCount(productId);

        // When
        productMetricsService.decrementLikeCount(productId);

        // Then
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(1);
    }

    @Test
    void 조회수를_증가시킨다() {
        // Given
        Long productId = 3L;

        // When
        productMetricsService.incrementViewCount(productId);
        productMetricsService.incrementViewCount(productId);

        // Then
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseThrow();
        assertThat(metrics.getViewCount()).isEqualTo(2);
    }

    @Test
    void 주문수와_판매금액을_증가시킨다() {
        // Given
        Long productId = 4L;
        int quantity = 3;
        BigDecimal amount = new BigDecimal("30000");

        // When
        productMetricsService.incrementOrderCount(productId, quantity, amount);

        // Then
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
            .orElseThrow();
        assertThat(metrics.getOrderCount()).isEqualTo(3);
        assertThat(metrics.getSalesAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }
}
