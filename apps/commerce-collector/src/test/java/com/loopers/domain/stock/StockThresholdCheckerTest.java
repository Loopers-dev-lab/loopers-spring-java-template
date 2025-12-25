package com.loopers.domain.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StockThresholdCheckerTest {

    private StockThresholdChecker stockThresholdChecker;

    @BeforeEach
    void setUp() {
        stockThresholdChecker = new StockThresholdChecker();
        ReflectionTestUtils.setField(stockThresholdChecker, "thresholdQuantity", 10);
        ReflectionTestUtils.setField(stockThresholdChecker, "thresholdRate", 0.1);
    }

    @Test
    @DisplayName("재고가 0개면 임계값 도달")
    void 재고_0개_임계값_도달() {
        // Given
        int currentStock = 0;

        // When
        boolean result = stockThresholdChecker.isBelowThreshold(currentStock);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("재고가 10개 이하면 임계값 도달")
    void 재고_10개_이하_임계값_도달() {
        // Given
        int currentStock = 10;

        // When
        boolean result = stockThresholdChecker.isBelowThreshold(currentStock);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("재고가 11개면 임계값 미도달")
    void 재고_11개_임계값_미도달() {
        // Given
        int currentStock = 11;

        // When
        boolean result = stockThresholdChecker.isBelowThreshold(currentStock);

        // Then
        assertThat(result).isFalse();
    }
}
