package com.loopers.batch.job.ranking.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

@DisplayName("DateRangeParser 단위 테스트")
class DateRangeParserUnitTest {

    private final DateRangeParser parser = new DateRangeParser();

    @Nested
    @DisplayName("주간 날짜 범위 파싱")
    class 주간_날짜_범위_파싱 {

        @Test
        @DisplayName("유효한 yearWeek 형식을 올바르게 파싱한다")
        void should_parse_valid_year_week_correctly() {
            // given
            String yearWeek = "2024-W52";

            // when
            LocalDate[] dateRange = parser.parseYearWeek(yearWeek);

            // then
            Assertions.assertThat(dateRange).hasSize(2);
            Assertions.assertThat(dateRange[0]).isBefore(dateRange[1]);
            Assertions.assertThat(dateRange[1]).isEqualTo(dateRange[0].plusDays(6));
        }

        @Test
        @DisplayName("2024년 1주차를 올바르게 파싱한다")
        void should_parse_first_week_of_2024_correctly() {
            // given
            String yearWeek = "2024-W1";

            // when
            LocalDate[] dateRange = parser.parseYearWeek(yearWeek);

            // then
            Assertions.assertThat(dateRange).hasSize(2);
            // 2024년 1주차는 1월 1일(월요일)부터 시작
            Assertions.assertThat(dateRange[0]).isEqualTo(LocalDate.of(2024, 1, 1));
            Assertions.assertThat(dateRange[1]).isEqualTo(LocalDate.of(2024, 1, 7));
        }

        @Test
        @DisplayName("null yearWeek에 대해 예외가 발생한다")
        void should_throw_exception_when_year_week_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() -> parser.parseYearWeek(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 yearWeek 형식입니다");
        }

        @Test
        @DisplayName("잘못된 yearWeek 형식에 대해 예외가 발생한다")
        void should_throw_exception_when_year_week_format_is_invalid() {
            // given
            String invalidYearWeek = "2024-52";

            // when & then
            Assertions.assertThatThrownBy(() -> parser.parseYearWeek(invalidYearWeek))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 yearWeek 형식입니다");
        }

        @Test
        @DisplayName("빈 문자열에 대해 예외가 발생한다")
        void should_throw_exception_when_year_week_is_empty() {
            // given & when & then
            Assertions.assertThatThrownBy(() -> parser.parseYearWeek(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 yearWeek 형식입니다");
        }
    }

    @Nested
    @DisplayName("월간 날짜 범위 파싱")
    class 월간_날짜_범위_파싱 {

        @Test
        @DisplayName("유효한 yearMonth 형식을 올바르게 파싱한다")
        void should_parse_valid_year_month_correctly() {
            // given
            String yearMonth = "2024-12";

            // when
            LocalDate[] dateRange = parser.parseYearMonth(yearMonth);

            // then
            Assertions.assertThat(dateRange).hasSize(2);
            Assertions.assertThat(dateRange[0]).isEqualTo(LocalDate.of(2024, 12, 1));
            Assertions.assertThat(dateRange[1]).isEqualTo(LocalDate.of(2024, 12, 31));
        }

        @Test
        @DisplayName("2월(윤년)을 올바르게 파싱한다")
        void should_parse_february_in_leap_year_correctly() {
            // given
            String yearMonth = "2024-02"; // 2024년은 윤년

            // when
            LocalDate[] dateRange = parser.parseYearMonth(yearMonth);

            // then
            Assertions.assertThat(dateRange[0]).isEqualTo(LocalDate.of(2024, 2, 1));
            Assertions.assertThat(dateRange[1]).isEqualTo(LocalDate.of(2024, 2, 29)); // 윤년
        }

        @Test
        @DisplayName("2월(평년)을 올바르게 파싱한다")
        void should_parse_february_in_non_leap_year_correctly() {
            // given
            String yearMonth = "2023-02"; // 2023년은 평년

            // when
            LocalDate[] dateRange = parser.parseYearMonth(yearMonth);

            // then
            Assertions.assertThat(dateRange[0]).isEqualTo(LocalDate.of(2023, 2, 1));
            Assertions.assertThat(dateRange[1]).isEqualTo(LocalDate.of(2023, 2, 28)); // 평년
        }

        @Test
        @DisplayName("null yearMonth에 대해 예외가 발생한다")
        void should_throw_exception_when_year_month_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() -> parser.parseYearMonth(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 yearMonth 형식입니다");
        }

        @Test
        @DisplayName("잘못된 yearMonth 형식에 대해 예외가 발생한다")
        void should_throw_exception_when_year_month_format_is_invalid() {
            // given
            String invalidYearMonth = "2024/12";

            // when & then
            Assertions.assertThatThrownBy(() -> parser.parseYearMonth(invalidYearMonth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 yearMonth 형식입니다");
        }

        @Test
        @DisplayName("존재하지 않는 월에 대해 예외가 발생한다")
        void should_throw_exception_when_month_does_not_exist() {
            // given
            String invalidYearMonth = "2024-13";

            // when & then
            Assertions.assertThatThrownBy(() -> parser.parseYearMonth(invalidYearMonth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yearMonth 파싱 중 오류가 발생했습니다");
        }
    }
}