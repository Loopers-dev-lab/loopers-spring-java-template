package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingAggregationInfo;
import com.loopers.application.ranking.RankingAggregationService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings/batch")
public class RankingBatchV1Controller implements RankingBatchV1ApiSpec {

    private final RankingAggregationService rankingAggregationService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping("/weekly")
    @Override
    public ApiResponse<RankingBatchV1Dto.BatchExecutionResponse> executeWeeklyRanking(
            @RequestParam(value = "targetDate", required = false) String targetDate
    ) {
        ZonedDateTime date = parseDateTime(targetDate);
        RankingAggregationInfo result = rankingAggregationService.executeWeeklyRanking(date);
        return ApiResponse.success(RankingBatchV1Dto.BatchExecutionResponse.from(result));
    }

    @PostMapping("/monthly")
    @Override
    public ApiResponse<RankingBatchV1Dto.BatchExecutionResponse> executeMonthlyRanking(
            @RequestParam(value = "targetDate", required = false) String targetDate
    ) {
        ZonedDateTime date = parseDateTime(targetDate);
        RankingAggregationInfo result = rankingAggregationService.executeMonthlyRanking(date);
        return ApiResponse.success(RankingBatchV1Dto.BatchExecutionResponse.from(result));
    }

    @PostMapping("/weekly-and-monthly")
    @Override
    public ApiResponse<RankingBatchV1Dto.BatchExecutionResponse> executeWeeklyAndMonthlyRanking(
            @RequestParam(value = "targetDate", required = false) String targetDate
    ) {
        ZonedDateTime date = parseDateTime(targetDate);
        RankingAggregationInfo result = rankingAggregationService.executeWeeklyAndMonthlyRanking(date);
        return ApiResponse.success(RankingBatchV1Dto.BatchExecutionResponse.from(result));
    }

    /**
     * 날짜 문자열을 ZonedDateTime으로 파싱
     * 미지정 시 현재 시간 반환
     * yyyy-MM-dd 형식 또는 ISO_ZONED_DATE_TIME 형식 지원
     */
    private ZonedDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return ZonedDateTime.now();
        }

        try {
            // ISO_ZONED_DATE_TIME 형식 시도
            return ZonedDateTime.parse(dateStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                // yyyy-MM-dd 형식 시도 (자정으로 변환)
                return java.time.LocalDate.parse(dateStr, DATE_FORMATTER)
                        .atStartOfDay(java.time.ZoneId.systemDefault());
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException(
                        "날짜 형식이 올바르지 않습니다. yyyy-MM-dd 또는 ISO_ZONED_DATE_TIME 형식으로 입력해주세요. 입력값: " + dateStr
                );
            }
        }
    }
}

