package com.loopers.batch.job.ranking.dto;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.batch.job.ranking.support.ScoreCalculator;

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
            Object[] row = {1L, 100L, 50L, 10L, 5L}; // productId, view, like, sales, order

            // when
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);

            // then
            Assertions.assertThat(aggregation.getProductId()).isEqualTo(1L);
            Assertions.assertThat(aggregation.getViewCount()).isEqualTo(100L);
            Assertions.assertThat(aggregation.getLikeCount()).isEqualTo(50L);
            Assertions.assertThat(aggregation.getSalesCount()).isEqualTo(10L);
            Assertions.assertThat(aggregation.getOrderCount()).isEqualTo(5L);
            // score = 100*1 + 50*3 + 10*5 + 5*2 = 310
            Assertions.assertThat(aggregation.getTotalScore()).isEqualTo(310L);
            Assertions.assertThat(aggregation.getRankPosition()).isEqualTo(0); // 초기값
        }

        @Test
        @DisplayName("null 배열에 대해 예외가 발생한다")
        void should_throw_exception_when_row_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() -> RankingAggregation.from(null, calculator))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("집계 결과 배열이 null이거나 길이가 부족합니다");
        }

        @Test
        @DisplayName("길이가 부족한 배열에 대해 예외가 발생한다")
        void should_throw_exception_when_row_length_is_insufficient() {
            // given
            Object[] shortRow = {1L, 100L, 50L}; // 길이 3 (5 미만)

            // when & then
            Assertions.assertThatThrownBy(() -> RankingAggregation.from(shortRow, calculator))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("집계 결과 배열이 null이거나 길이가 부족합니다");
        }

        @Test
        @DisplayName("잘못된 데이터 타입에 대해 예외가 발생한다")
        void should_throw_exception_when_data_type_is_invalid() {
            // given
            Object[] invalidRow = {"invalid", 100L, 50L, 10L, 5L}; // productId가 String

            // when & then
            Assertions.assertThatThrownBy(() -> RankingAggregation.from(invalidRow, calculator))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("집계 결과 데이터 형식이 올바르지 않습니다");
        }

        @Test
        @DisplayName("Number 타입의 다양한 형태를 처리한다")
        void should_handle_various_number_types() {
            // given - Integer, Long, BigDecimal 등 다양한 Number 타입
            Object[] row = {1L, 100, 50L, 10, 5L};

            // when
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);

            // then
            Assertions.assertThat(aggregation.getViewCount()).isEqualTo(100L);
            Assertions.assertThat(aggregation.getLikeCount()).isEqualTo(50L);
            Assertions.assertThat(aggregation.getSalesCount()).isEqualTo(10L);
            Assertions.assertThat(aggregation.getOrderCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("순위 부여")
    class 순위_부여 {

        @Test
        @DisplayName("유효한 순위를 부여한다")
        void should_assign_valid_rank() {
            // given
            Object[] row = {1L, 100L, 50L, 10L, 5L};
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);

            // when
            aggregation.assignRank(1);

            // then
            Assertions.assertThat(aggregation.getRankPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("100위까지 순위를 부여할 수 있다")
        void should_assign_rank_up_to_100() {
            // given
            Object[] row = {1L, 100L, 50L, 10L, 5L};
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);

            // when
            aggregation.assignRank(100);

            // then
            Assertions.assertThat(aggregation.getRankPosition()).isEqualTo(100);
        }

        @Test
        @DisplayName("0 이하의 순위에 대해 예외가 발생한다")
        void should_throw_exception_when_rank_is_zero_or_negative() {
            // given
            Object[] row = {1L, 100L, 50L, 10L, 5L};
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);

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
            Object[] row = {1L, 100L, 50L, 10L, 5L};
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);

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
            Object[] row = {1L, 100L, 50L, 10L, 5L};
            RankingAggregation aggregation = RankingAggregation.from(row, calculator);
            aggregation.assignRank(1);

            // when
            String result = aggregation.toString();

            // then
            Assertions.assertThat(result).contains("productId=1");
            Assertions.assertThat(result).contains("score=310");
            Assertions.assertThat(result).contains("rank=1");
        }
    }
}
