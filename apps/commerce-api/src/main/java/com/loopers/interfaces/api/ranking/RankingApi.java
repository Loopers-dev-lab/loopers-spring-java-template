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
 * 랭킹 API
 */
@Tag(name = "Ranking", description = "상품 랭킹 API")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingApi {

    private final RankingFacade rankingFacade;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Operation(
        summary = "일간 랭킹 조회",
        description = "특정 날짜의 상품 랭킹을 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<RankingResponse> getRankings(
        @Parameter(description = "조회 날짜 (yyyyMMdd), 미입력 시 오늘", example = "20250123")
        @RequestParam(required = false) String date,

        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") int page,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        // 날짜 검증
        String targetDate = validateAndGetDate(date);

        // 랭킹 조회
        List<RankingProductInfo> rankings = rankingFacade.getDailyRanking(targetDate, page, size);

        return ResponseEntity.ok(new RankingResponse(
            targetDate,
            page,
            size,
            rankings
        ));
    }

    /**
     * 날짜 검증 및 변환
     */
    private String validateAndGetDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now().format(DATE_FORMATTER);
        }

        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return date;
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. (yyyyMMdd)");
        }
    }
}
