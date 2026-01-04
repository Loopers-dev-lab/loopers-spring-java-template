package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.rank.ProductRank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductRankAggregationProcessor 테스트.
 */
class ProductRankAggregationProcessorTest {

    private final ProductRankAggregationProcessor processor = new ProductRankAggregationProcessor();

    @DisplayName("주간 기간 정보를 설정할 수 있다")
    @Test
    void setsWeeklyPeriod() {
        // arrange
        LocalDate targetDate = LocalDate.of(2024, 12, 15); // 일요일
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;

        // act
        processor.setPeriod(periodType, targetDate);

        // assert
        assertThat(processor.getPeriodType()).isEqualTo(periodType);
        assertThat(processor.getPeriodStartDate()).isEqualTo(LocalDate.of(2024, 12, 9)); // 월요일
    }

    @DisplayName("월간 기간 정보를 설정할 수 있다")
    @Test
    void setsMonthlyPeriod() {
        // arrange
        LocalDate targetDate = LocalDate.of(2024, 12, 15);
        ProductRank.PeriodType periodType = ProductRank.PeriodType.MONTHLY;

        // act
        processor.setPeriod(periodType, targetDate);

        // assert
        assertThat(processor.getPeriodType()).isEqualTo(periodType);
        assertThat(processor.getPeriodStartDate()).isEqualTo(LocalDate.of(2024, 12, 1)); // 월의 1일
    }

    @DisplayName("주간 기간 설정 시 해당 주의 월요일을 시작일로 계산한다")
    @Test
    void calculatesWeekStartAsMonday_whenSettingWeeklyPeriod() {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.WEEKLY;
        
        // 월요일
        LocalDate monday = LocalDate.of(2024, 12, 9);
        // 수요일
        LocalDate wednesday = LocalDate.of(2024, 12, 11);
        // 일요일
        LocalDate sunday = LocalDate.of(2024, 12, 15);

        // act & assert
        processor.setPeriod(periodType, monday);
        assertThat(processor.getPeriodStartDate()).isEqualTo(monday);

        processor.setPeriod(periodType, wednesday);
        assertThat(processor.getPeriodStartDate()).isEqualTo(monday);

        processor.setPeriod(periodType, sunday);
        assertThat(processor.getPeriodStartDate()).isEqualTo(monday);
    }

    @DisplayName("월간 기간 설정 시 해당 월의 1일을 시작일로 계산한다")
    @Test
    void calculatesMonthStartAsFirstDay_whenSettingMonthlyPeriod() {
        // arrange
        ProductRank.PeriodType periodType = ProductRank.PeriodType.MONTHLY;
        LocalDate expectedStart = LocalDate.of(2024, 12, 1);
        
        // 1일
        LocalDate firstDay = LocalDate.of(2024, 12, 1);
        // 15일
        LocalDate midDay = LocalDate.of(2024, 12, 15);
        // 마지막 일
        LocalDate lastDay = LocalDate.of(2024, 12, 31);

        // act & assert
        processor.setPeriod(periodType, firstDay);
        assertThat(processor.getPeriodStartDate()).isEqualTo(expectedStart);

        processor.setPeriod(periodType, midDay);
        assertThat(processor.getPeriodStartDate()).isEqualTo(expectedStart);

        processor.setPeriod(periodType, lastDay);
        assertThat(processor.getPeriodStartDate()).isEqualTo(expectedStart);
    }

    @DisplayName("기간 정보를 여러 번 설정할 수 있다")
    @Test
    void canSetPeriodMultipleTimes() {
        // arrange
        LocalDate firstDate = LocalDate.of(2024, 12, 15);
        LocalDate secondDate = LocalDate.of(2024, 11, 20);

        // act
        processor.setPeriod(ProductRank.PeriodType.WEEKLY, firstDate);
        ProductRank.PeriodType firstType = processor.getPeriodType();
        LocalDate firstStart = processor.getPeriodStartDate();

        processor.setPeriod(ProductRank.PeriodType.MONTHLY, secondDate);
        ProductRank.PeriodType secondType = processor.getPeriodType();
        LocalDate secondStart = processor.getPeriodStartDate();

        // assert
        assertThat(firstType).isEqualTo(ProductRank.PeriodType.WEEKLY);
        assertThat(firstStart).isEqualTo(LocalDate.of(2024, 12, 9));

        assertThat(secondType).isEqualTo(ProductRank.PeriodType.MONTHLY);
        assertThat(secondStart).isEqualTo(LocalDate.of(2024, 11, 1));
    }
}

