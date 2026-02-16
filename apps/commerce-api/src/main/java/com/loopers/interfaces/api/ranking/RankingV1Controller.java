package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingType;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @GetMapping
    public ApiResponse<RankingV1Dto.PagingRankingResponse> getRankingWithPaging(
            RankingV1Dto.GetRankingWithPagingRequest request
    ) {
        LocalDate date = parseDate(request.date());

        List<RankingInfo> rankings = rankingFacade.getRankingWithPaging(
                request.type(),
                request.periodType(),
                date,
                request.page(),
                request.size()
        );

        long totalCount = rankingFacade.getTotalRankingCount(request.type(), date);

        return ApiResponse.success(
                RankingV1Dto.PagingRankingResponse.of(
                        rankings,
                        request.page(),
                        request.size(),
                        totalCount
                )
        );
    }

    @Override
    @GetMapping("/top")
    public ApiResponse<RankingV1Dto.TopRankingResponse> getTopRanking(
            RankingV1Dto.GetTopRankingRequest request
    ) {
        LocalDate date = parseDate(request.date());

        List<RankingInfo> rankings = rankingFacade.getTopRanking(
                request.type(),
                request.periodType(),
                date,
                request.limit()
        );

        long totalCount = rankingFacade.getTotalRankingCount(request.type(), date);

        return ApiResponse.success(
                RankingV1Dto.TopRankingResponse.of(
                        rankings,
                        totalCount
                )
        );
    }

    @Override
    @GetMapping("/products/{productId}")
    public ApiResponse<RankingV1Dto.ProductRankingResponse> getProductRanking(
            @PathVariable Long productId,
            @RequestParam RankingType type,
            @RequestParam(required = false) String date
    ) {
        LocalDate targetDate = parseDate(date);

        RankingInfo ranking = rankingFacade.getProductRanking(type, targetDate, productId);

        if (ranking == null) {
            return ApiResponse.success(null);  // 또는 적절한 에러 응답
        }

        return ApiResponse.success(
                RankingV1Dto.ProductRankingResponse.from(ranking)
        );
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }
}
