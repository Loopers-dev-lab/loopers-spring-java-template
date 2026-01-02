package com.loopers.batch.job.ranking.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScoreCalculator 단위 테스트")
class ScoreCalculatorUnitTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    @Nested
    @DisplayName("점수 계산")
    class 점수_계산 {

        @Test
        @DisplayName("가중치가 올바르게 적용되어 점수가 계산된다")
        void should_calculate_score_with_correct_weights() {
            // given
            long viewCount = 100, likeCount = 50, salesCount = 10, orderCount = 5;

            // when
            long score = calculator.calculate(viewCount, likeCount, salesCount, orderCount);

            // then
            Assertions.assertThat(score).isEqualTo(214L);
        }

        @Test
        @DisplayName("모든 메트릭이 0인 경우 점수는 0이다")
        void should_return_zero_when_all_metrics_are_zero() {
            // given & when
            long score = calculator.calculate(0, 0, 0, 0);

            // then
            Assertions.assertThat(score).isEqualTo(0L);
        }

        @Test
        @DisplayName("판매수량이 가장 높은 가중치를 가진다")
        void should_have_highest_weight_for_sales_count() {
            // given
            long singleSale = calculator.calculate(0, 0, 1, 0);
            long singleView = calculator.calculate(1, 0, 0, 0);
            long singleLike = calculator.calculate(0, 1, 0, 0);
            long singleOrder = calculator.calculate(0, 0, 0, 1);

            // when & then
            Assertions.assertThat(singleSale).isGreaterThan(singleView);
            Assertions.assertThat(singleSale).isGreaterThan(singleLike);
        }

        @Test
        @DisplayName("큰 숫자에서도 정확히 계산된다")
        void should_calculate_correctly_with_large_numbers() {
            // given
            long viewCount = 1_000_000L;
            long likeCount = 500_000L;
            long salesCount = 100_000L;
            long orderCount = 50_000L;

            // when
            long score = calculator.calculate(viewCount, likeCount, salesCount, orderCount);

            // then
            long expected = (long) (((1_000_000L * 0.1) + (500_000L * 0.2) + Math.log1p(100_000L) *0.6 ) * 10);
            Assertions.assertThat(score).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("가중치 정보")
    class 가중치_정보 {

        @Test
        @DisplayName("가중치 정보를 올바른 형식으로 반환한다")
        void should_return_weight_info_in_correct_format() {
            // when
            String weightInfo = calculator.getWeightInfo();

            // then
            Assertions.assertThat(weightInfo).contains("VIEW=0.1");
            Assertions.assertThat(weightInfo).contains("LIKE=0.2");
            Assertions.assertThat(weightInfo).contains("SALES=0.6");
        }
    }
}
