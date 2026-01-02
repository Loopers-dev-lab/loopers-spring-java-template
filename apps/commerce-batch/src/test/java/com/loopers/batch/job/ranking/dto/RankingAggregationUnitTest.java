package com.loopers.batch.job.ranking.dto;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.batch.job.ranking.support.ScoreCalculator;
import com.loopers.domain.metrics.ProductMetricsAggregation;

@DisplayName("RankingAggregation 단위 테스트")
class RankingAggregationUnitTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    @Nested
    @DisplayName("집계 결과로부터 생성")
    class 집계_결과로부터_생성 {

        @Test
        @DisplayName("유효한 집계 결과로부터 객체를 생성한다")
        void should_create_from_valid_aggregation_result() {
            // given
            ProductMetricsAggregation metrics = new ProductMetricsAggregation(
                    1L, 100L, 50L, 10L, 5L, java.math.BigDecimal.valueOf(1000)
            );

            // when
            RankingAggregation aggregation = RankingAggregation.from(metrics, calculator);

            // then
            Assertions.assertThat(aggregation.getProductId()).isEqualTo(1L);
            Assertions.assertThat(aggregation.getViewCount()).isEqualTo(100L);
            Assertions.assertThat(aggregation.getLikeCount()).isEqualTo(50L);
            Assertions.assertThat(aggregation.getSalesCount()).isEqualTo(10L);
            Assertions.assertThat(aggregation.getOrderCount()).isEqualTo(5L);
            Assertions.assertThat(aggregation.getTotalSalesAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(1000));
            // score = (100*0.1 + 50*0.2 + log(1001)*0.6) * 10 = (10+10+4.145) * 10 = 241
            Assertions.assertThat(aggregation.getTotalScore()).isEqualTo(241L);
            Assertions.assertThat(aggregation.getRankPosition()).isEqualTo(0); // 초기값
        }

        @Test
        @DisplayName("null 메트릭에 대해 예외가 발생한다")
        void should_throw_exception_when_metrics_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() -> RankingAggregation.from(null, calculator))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("집계 결과(metrics)가 null입니다.");
        }
    }

    @Nested
    @DisplayName("순위 부여")
    class 순위_부여 {

        @Test
        @DisplayName("유효한 순위를 부여한다")
        void should_assign_valid_rank() {
            // given
            ProductMetricsAggregation metrics = new ProductMetricsAggregation(
                    1L, 100L, 50L, 10L, 5L, java.math.BigDecimal.valueOf(1000)
            );
            RankingAggregation aggregation = RankingAggregation.from(metrics, calculator);

            // when
            aggregation.assignRank(1);

            // then
            Assertions.assertThat(aggregation.getRankPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("100위까지 순위를 부여할 수 있다")
        void should_assign_rank_up_to_100() {
            // given
            ProductMetricsAggregation metrics = new ProductMetricsAggregation(
                    1L, 100L, 50L, 10L, 5L, java.math.BigDecimal.valueOf(1000)
            );
            RankingAggregation aggregation = RankingAggregation.from(metrics, calculator);

            // when
            aggregation.assignRank(100);

            // then
            Assertions.assertThat(aggregation.getRankPosition()).isEqualTo(100);
        }

        @Test
        @DisplayName("0 이하의 순위에 대해 예외가 발생한다")
        void should_throw_exception_when_rank_is_zero_or_negative() {
            // given
            ProductMetricsAggregation metrics = new ProductMetricsAggregation(
                    1L, 100L, 50L, 10L, 5L, java.math.BigDecimal.valueOf(1000)
            );
            RankingAggregation aggregation = RankingAggregation.from(metrics, calculator);

            // when & then
            Assertions.assertThatThrownBy(() -> aggregation.assignRank(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("순위는 1~100 범위여야 합니다");

            Assertions.assertThatThrownBy(() -> aggregation.assignRank(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("순위는 1~100 범위여야 합니다");
        }

        @Test
        @DisplayName("100을 초과하는 순위에 대해 예외가 발생한다")
        void should_throw_exception_when_rank_exceeds_100() {
            // given
            ProductMetricsAggregation metrics = new ProductMetricsAggregation(
                    1L, 100L, 50L, 10L, 5L, java.math.BigDecimal.valueOf(1000)
            );
            RankingAggregation aggregation = RankingAggregation.from(metrics, calculator);

            // when & then
            Assertions.assertThatThrownBy(() -> aggregation.assignRank(101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("순위는 1~100 범위여야 합니다");
        }
    }

    @Nested
    @DisplayName("문자열 표현")
    class 문자열_표현 {

        @Test
        @DisplayName("toString이 올바른 형식을 반환한다")
        void should_return_correct_string_format() {
            // given
            ProductMetricsAggregation metrics = new ProductMetricsAggregation(
                    1L, 100L, 50L, 10L, 5L, java.math.BigDecimal.valueOf(1000)
            );
            RankingAggregation aggregation = RankingAggregation.from(metrics, calculator);
            aggregation.assignRank(1);

            // when
            String result = aggregation.toString();

            // then
            Assertions.assertThat(result).contains("productId=1");
            Assertions.assertThat(result).contains("score=241");
            Assertions.assertThat(result).contains("rank=1");
        }
    }
}
