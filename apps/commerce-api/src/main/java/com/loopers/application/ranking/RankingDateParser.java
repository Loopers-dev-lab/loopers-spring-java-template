package com.loopers.application.ranking;

import lombok.Getter;

@Getter
public class RankingDateParser {
    private final String originalDate;
    private final RankingPeriod period;
    private final String convertedDate;

    private RankingDateParser(String originalDate, RankingPeriod period, String convertedDate) {
        this.originalDate = originalDate;
        this.period = period;
        this.convertedDate = convertedDate;
    }

    /**
     * date 파라미터를 분석하여 기간과 변환된 날짜를 반환
     * 
     * @param date 입력 날짜
     * - YYYYMMDD (8자리) -> 일간 랭킹 (예: 20251225)
     * - YYYYWWW (6자리 + W) -> 주간 랭킹 (예: 202552W)
     * - YYYYMMM (6자리 + M) -> 월간 랭킹 (예: 202512M)
     * @return RankingDateParser 객체
     */
    public static RankingDateParser parse(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("날짜가 비어있습니다.");
        }

        String trimmedDate = date.trim().toUpperCase();
        
        if (trimmedDate.length() == 8 && trimmedDate.matches("\\d{8}")) {
            // YYYYMMDD -> 일간 랭킹
            validateDailyDate(trimmedDate);
            return new RankingDateParser(trimmedDate, RankingPeriod.DAILY, trimmedDate);
        }
        
        if (trimmedDate.length() == 7) {
            char suffix = trimmedDate.charAt(6);
            String datePart = trimmedDate.substring(0, 6);
            
            if (!datePart.matches("\\d{6}")) {
                throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다: " + trimmedDate);
            }
            
            switch (suffix) {
                case 'W':
                    // YYYYWWW -> 주간 랭킹
                    return parseWeeklyDate(trimmedDate, datePart);
                    
                case 'M':
                    // YYYYMMM -> 월간 랭킹
                    return parseMonthlyDate(trimmedDate, datePart);
                    
                default:
                    throw new IllegalArgumentException(
                        "지원하지 않는 접미사입니다: " + suffix + 
                        ". 지원 접미사: W(주간), M(월간)"
                    );
            }
        }
        
        throw new IllegalArgumentException(
            "지원하지 않는 날짜 형식입니다: " + trimmedDate + 
            ". 지원 형식: YYYYMMDD(일간), YYYYWWW(주간), YYYYMMM(월간)"
        );
    }

    private static RankingDateParser parseWeeklyDate(String originalDate, String datePart) {
        try {
            String year = datePart.substring(0, 4);
            String week = datePart.substring(4, 6);
            
            int yearInt = Integer.parseInt(year);
            int weekInt = Integer.parseInt(week);
            
            validateYear(yearInt);
            
            if (weekInt < 1 || weekInt > 53) {
                throw new IllegalArgumentException("유효하지 않은 주차입니다: " + weekInt + " (1~53 범위)");
            }
            
            // 주간 랭킹용 날짜로 변환 (해당 주차의 대표 날짜)
            String convertedDate = convertWeekToDate(yearInt, weekInt);
            return new RankingDateParser(originalDate, RankingPeriod.WEEKLY, convertedDate);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("주간 날짜 형식이 올바르지 않습니다: " + originalDate);
        }
    }

    private static RankingDateParser parseMonthlyDate(String originalDate, String datePart) {
        try {
            String year = datePart.substring(0, 4);
            String month = datePart.substring(4, 6);
            
            int yearInt = Integer.parseInt(year);
            int monthInt = Integer.parseInt(month);
            
            validateYear(yearInt);
            
            if (monthInt < 1 || monthInt > 12) {
                throw new IllegalArgumentException("유효하지 않은 월입니다: " + monthInt + " (1~12 범위)");
            }
            
            // 월간 랭킹용 날짜로 변환 (해당 월의 마지막 날)
            String convertedDate = convertMonthToDate(yearInt, monthInt);
            return new RankingDateParser(originalDate, RankingPeriod.MONTHLY, convertedDate);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("월간 날짜 형식이 올바르지 않습니다: " + originalDate);
        }
    }

    private static void validateDailyDate(String date) {
        try {
            String year = date.substring(0, 4);
            String month = date.substring(4, 6);
            String day = date.substring(6, 8);
            
            int yearInt = Integer.parseInt(year);
            int monthInt = Integer.parseInt(month);
            int dayInt = Integer.parseInt(day);
            
            validateYear(yearInt);
            
            if (monthInt < 1 || monthInt > 12) {
                throw new IllegalArgumentException("유효하지 않은 월입니다: " + monthInt);
            }
            
            if (dayInt < 1 || dayInt > 31) {
                throw new IllegalArgumentException("유효하지 않은 일입니다: " + dayInt);
            }
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("일간 날짜 형식이 올바르지 않습니다: " + date);
        }
    }

    private static void validateYear(int year) {
        if (year < 2020 || year > 2030) {
            throw new IllegalArgumentException("지원하지 않는 연도입니다: " + year + " (2020~2030 범위)");
        }
    }

    private static String convertWeekToDate(int year, int week) {
        // 해당 주차의 대표 날짜 계산 (간단한 근사치)
        // 1월 첫째 주를 기준으로 계산
        int dayOfYear = (week - 1) * 7 + 4; // 해당 주의 목요일
        
        // 윤년 고려
        int[] monthDays = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year)) {
            monthDays[1] = 29;
        }
        
        int month = 1;
        while (month <= 12 && dayOfYear > monthDays[month - 1]) {
            dayOfYear -= monthDays[month - 1];
            month++;
        }
        
        // 범위 초과 시 12월 마지막 날로 조정
        if (month > 12) {
            month = 12;
            dayOfYear = monthDays[11];
        }
        
        return String.format("%04d%02d%02d", year, month, dayOfYear);
    }

    private static String convertMonthToDate(int year, int month) {
        // 해당 월의 마지막 날로 변환
        int[] monthDays = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year) && month == 2) {
            monthDays[1] = 29;
        }
        
        int lastDay = monthDays[month - 1];
        return String.format("%04d%02d%02d", year, month, lastDay);
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * 원본 기간 정보 반환 (MV 조회용)
     */
    public String getPeriodKey() {
        switch (period) {
            case DAILY:
                return originalDate;
            case WEEKLY:
                // 202552W -> 202552 (YYYYWW 형식으로 변환)
                return originalDate.substring(0, 6);
            case MONTHLY:
                // 202512M -> 202512 (YYYYMM 형식으로 변환)
                return originalDate.substring(0, 6);
            default:
                throw new IllegalStateException("알 수 없는 기간입니다: " + period);
        }
    }
}