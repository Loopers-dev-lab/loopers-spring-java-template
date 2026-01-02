package com.loopers.batch.job.ranking.support;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;

import org.springframework.stereotype.Component;

/**
 * 날짜 범위 파싱 유틸리티
 * - yearWeek (e.g., "2024-W52") → 주간 날짜 범위
 * - yearMonth (e.g., "2024-12") → 월간 날짜 범위
 */
@Component
public class DateRangeParser {

    /**
     * yearWeek 문자열을 주간 날짜 범위로 변환합니다.
     *
     * @param yearWeek "2024-W52" 형식의 문자열
     * @return [startDate, endDate] 배열
     * @throws IllegalArgumentException 잘못된 형식인 경우
     */
    public LocalDate[] parseYearWeek(String yearWeek) {
        if (yearWeek == null || !yearWeek.matches("\\d{4}-W\\d{1,2}")) {
            throw new IllegalArgumentException(
                    String.format("잘못된 yearWeek 형식입니다. 예상: '2024-W52', 실제: '%s'", yearWeek));
        }

        try {
            String[] parts = yearWeek.split("-W");
            int year = Integer.parseInt(parts[0]);
            int week = Integer.parseInt(parts[1]);

            // ISO 주차 시스템 사용 (월요일 시작)
            WeekFields weekFields = WeekFields.ISO;
            LocalDate startOfWeek = LocalDate.of(year, 1, 1)
                    .with(weekFields.weekOfYear(), week)
                    .with(weekFields.dayOfWeek(), 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            return new LocalDate[]{startOfWeek, endOfWeek};
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("yearWeek 파싱 중 오류가 발생했습니다: %s", yearWeek), e);
        }
    }

    /**
     * yearMonth 문자열을 월간 날짜 범위로 변환합니다.
     *
     * @param yearMonth "2024-12" 형식의 문자열
     * @return [startDate, endDate] 배열
     * @throws IllegalArgumentException 잘못된 형식인 경우
     */
    public LocalDate[] parseYearMonth(String yearMonth) {
        if (yearMonth == null || !yearMonth.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException(
                    String.format("잘못된 yearMonth 형식입니다. 예상: '2024-12', 실제: '%s'", yearMonth));
        }

        try {
            YearMonth ym = YearMonth.parse(yearMonth);
            LocalDate startOfMonth = ym.atDay(1);
            LocalDate endOfMonth = ym.atEndOfMonth();

            return new LocalDate[]{startOfMonth, endOfMonth};
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("yearMonth 파싱 중 오류가 발생했습니다: %s", yearMonth), e);
        }
    }
}
