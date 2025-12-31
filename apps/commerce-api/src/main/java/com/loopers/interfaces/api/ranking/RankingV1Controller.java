package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Ranking", description = "상품 랭킹 API")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @GetMapping
    public ApiResponse<RankingV1Dto.RankingPageResponse> getRankingPage(
            @Parameter(description = "조회 날짜 (yyyyMMdd), 미입력 시 오늘", example = "20250123")
            @RequestParam(required = false) String date,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        String targetDate = validateAndGetDate(date);

        List<RankingProductInfo> rankings = rankingFacade.getDailyRanking(targetDate, page, size);

        List<RankingV1Dto.RankingItem> items = rankings.stream()
                .map(p -> new RankingV1Dto.RankingItem(
                        p.productId(),
                        p.name(),
                        p.price(),
                        p.likeCount()
                ))
                .toList();

        return ApiResponse.success(new RankingV1Dto.RankingPageResponse(
                targetDate, page, size, items
        ));
    }

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
