package com.loopers.domain.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductMetrics 도메인 테스트")
class ProductMetricsTest {

    @Nested
    @DisplayName("좋아요 증감 로직")
    class LikeDeltaTests {

        @Test
        @DisplayName("같은 이벤트가 다시 들어오면 좋아요가 한 번만 반영된다")
        void applyLikeDelta_updatesOncePerVersion() {
            ProductMetrics metrics = ProductMetrics.initialize(1L);

            boolean applied = metrics.applyLikeDelta(2, 100L);
            boolean skipped = metrics.applyLikeDelta(1, 99L);
            boolean duplicateSkip = metrics.applyLikeDelta(1, 100L);

            assertThat(applied).isTrue();
            assertThat(skipped).isFalse();
            assertThat(duplicateSkip).isFalse();
            assertThat(metrics.getLikeCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("판매량 집계 로직")
    class SalesTests {

        @Test
        @DisplayName("이전 버전이나 동일 버전이면 판매량이 증가하지 않는다")
        void increaseSales_appliesOnlyForNewerVersion() {
            ProductMetrics metrics = ProductMetrics.initialize(2L);

            boolean applied = metrics.increaseSales(3, 200L);
            boolean skipped = metrics.increaseSales(5, 150L);
            boolean duplicateSkip = metrics.increaseSales(1, 200L);

            assertThat(applied).isTrue();
            assertThat(skipped).isFalse();
            assertThat(duplicateSkip).isFalse();
            assertThat(metrics.getSalesCount()).isEqualTo(3);
        }
    }
}
