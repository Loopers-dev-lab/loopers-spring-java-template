package com.loopers.domain.ranking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class PeriodUtils {
    
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    /**
     * 주어진 날짜가 속한 주의 시작일(월요일) 반환
     */
    public static LocalDate getWeekStartDate(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
    
    /**
     * 주어진 날짜가 속한 주의 종료일(일요일) 반환
     */
    public static LocalDate getWeekEndDate(LocalDate date) {
        return date.with(DayOfWeek.MONDAY).plusDays(6);
    }
    
    /**
     * 주어진 날짜가 속한 월의 시작일 반환
     */
    public static LocalDate getMonthStartDate(LocalDate date) {
        return YearMonth.from(date).atDay(1);
    }
    
    /**
     * 주어진 날짜가 속한 월의 마지막일 반환
     */
    public static LocalDate getMonthEndDate(LocalDate date) {
        return YearMonth.from(date).atEndOfMonth();
    }
    
    /**
     * 월간 키 생성 (YYYY-MM 형태)
     */
    public static String getMonthKey(LocalDate date) {
        return YearMonth.from(date).format(MONTH_FORMATTER);
    }
    
    /**
     * 문자열 날짜(yyyyMMdd)를 LocalDate로 변환
     */
    public static LocalDate parseDate(String dateString) {
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    /**
     * 주간 범위 정보
     */
    public record WeekRange(LocalDate start, LocalDate end) {
        public static WeekRange from(LocalDate date) {
            return new WeekRange(getWeekStartDate(date), getWeekEndDate(date));
        }
    }
    
    /**
     * 월간 범위 정보
     */
    public record MonthRange(LocalDate start, LocalDate end, String key) {
        public static MonthRange from(LocalDate date) {
            return new MonthRange(
                getMonthStartDate(date), 
                getMonthEndDate(date), 
                getMonthKey(date)
            );
        }
    }
}