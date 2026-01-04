package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 랭킹 조회 API v1 컨트롤러.
 * <p>
 * 랭킹 조회 유즈케이스를 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller {

    private final RankingService rankingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 랭킹을 조회합니다.
     * <p>
     * 기간별(일간/주간/월간) 랭킹을 페이징하여 조회합니다.
     * </p>
     * <p>
     * <b>기간 타입:</b>
     * <ul>
     *   <li>DAILY: 일간 랭킹 (Redis ZSET에서 조회)</li>
     *   <li>WEEKLY: 주간 랭킹 (Materialized View에서 조회)</li>
     *   <li>MONTHLY: 월간 랭킹 (Materialized View에서 조회)</li>
     * </ul>
     * </p>
     *
     * @param date 날짜 (yyyyMMdd 형식, 기본값: 오늘 날짜)
     * @param period 기간 타입 (DAILY, WEEKLY, MONTHLY, 기본값: DAILY)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지당 항목 수 (기본값: 20)
     * @return 랭킹 목록을 담은 API 응답
     */
    @GetMapping
    public ApiResponse<RankingV1Dto.RankingsResponse> getRankings(
        @RequestParam(required = false) String date,
        @RequestParam(required = false, defaultValue = "DAILY") String period,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // 날짜 파라미터 검증 및 기본값 처리
        LocalDate targetDate = parseDate(date);

        // 기간 타입 파싱 및 검증
        RankingService.PeriodType periodType = parsePeriodType(period);

        // 페이징 검증
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100; // 최대 100개로 제한
        }

        RankingService.RankingsResponse result = rankingService.getRankings(targetDate, periodType, page, size);
        return ApiResponse.success(RankingV1Dto.RankingsResponse.from(result));
    }

    /**
     * 날짜 문자열을 LocalDate로 파싱합니다.
     * <p>
     * 날짜가 없거나 파싱 실패 시 오늘 날짜를 반환합니다.
     * </p>
     *
     * @param dateStr 날짜 문자열 (yyyyMMdd 형식)
     * @return 파싱된 날짜 (실패 시 오늘 날짜)
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now(ZoneId.of("UTC"));
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // 파싱 실패 시 오늘 날짜 반환 (UTC 기준)
            return LocalDate.now(ZoneId.of("UTC"));
        }
    }

    /**
     * 기간 타입 문자열을 PeriodType으로 파싱합니다.
     * <p>
     * 파싱 실패 시 DAILY를 반환합니다.
     * </p>
     *
     * @param periodStr 기간 타입 문자열 (DAILY, WEEKLY, MONTHLY)
     * @return 파싱된 기간 타입 (실패 시 DAILY)
     */
    private RankingService.PeriodType parsePeriodType(String periodStr) {
        if (periodStr == null || periodStr.isBlank()) {
            return RankingService.PeriodType.DAILY;
        }

        try {
            return RankingService.PeriodType.valueOf(periodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 파싱 실패 시 DAILY 반환
            return RankingService.PeriodType.DAILY;
        }
    }
}
