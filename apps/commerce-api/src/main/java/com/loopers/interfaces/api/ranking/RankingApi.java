package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingFacade.RankingProductInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API controller for product rankings.
 *
 * <p>Provides endpoints for querying daily, weekly, and monthly product rankings.
 */
@Tag(name = "Ranking", description = "Product Ranking API")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingApi {

    private final RankingFacade rankingFacade;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Operation(
        summary = "Query product rankings",
        description = "Retrieves product rankings for a specific period (daily, weekly, or monthly) with pagination support."
    )
    @GetMapping
    public ResponseEntity<RankingResponse> getRankings(
        @Parameter(description = "Period type: DAILY, WEEKLY, MONTHLY", example = "DAILY")
        @RequestParam(required = false, defaultValue = "DAILY") String periodType,

        @Parameter(description = "Target date (yyyyMMdd) for daily rankings", example = "20250130")
        @RequestParam(required = false) String date,

        @Parameter(description = "Target week (YYYY-Wnn) for weekly rankings", example = "2025-W05")
        @RequestParam(required = false) String yearWeek,

        @Parameter(description = "Target month (YYYY-MM) for monthly rankings", example = "2025-01")
        @RequestParam(required = false) String yearMonth,

        @Parameter(description = "Page number (1-based)", example = "1")
        @RequestParam(defaultValue = "1") int page,

        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        String period;
        List<RankingProductInfo> rankings;

        switch (periodType.toUpperCase()) {
            case "WEEKLY":
                if (yearWeek == null || yearWeek.isBlank()) {
                    throw new IllegalArgumentException("yearWeek parameter is required for WEEKLY period type");
                }
                period = yearWeek;
                rankings = rankingFacade.getWeeklyRanking(yearWeek, page, size);
                break;

            case "MONTHLY":
                if (yearMonth == null || yearMonth.isBlank()) {
                    throw new IllegalArgumentException("yearMonth parameter is required for MONTHLY period type");
                }
                period = yearMonth;
                rankings = rankingFacade.getMonthlyRanking(yearMonth, page, size);
                break;

            case "DAILY":
            default:
                String targetDate = validateAndGetDate(date);
                period = targetDate;
                rankings = rankingFacade.getDailyRanking(targetDate, page, size);
                break;
        }

        return ResponseEntity.ok(new RankingResponse(
            period,
            page,
            size,
            rankings
        ));
    }

    /**
     * Validates and normalizes the date parameter.
     *
     * @param date the date string in yyyyMMdd format (nullable)
     * @return validated date string, defaults to today if null
     * @throws IllegalArgumentException if date format is invalid
     */
    private String validateAndGetDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now().format(DATE_FORMATTER);
        }

        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return date;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyyMMdd");
        }
    }
}
