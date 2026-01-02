package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingV1Dto;
import com.loopers.domain.ranking.Period;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {
    private final RankingFacade rankingFacade;

    @GetMapping
    @Override
    public ApiResponse<RankingV1Dto.ProductRankingPageResponse> getProductRanking(
            @RequestParam String date,
            @RequestParam(defaultValue = "DAILY") Period period,
            @RequestParam int size,
            @RequestParam int page
    ) {
        RankingV1Dto.ProductRankingPageResponse response = rankingFacade.getProductRanking(date, period, page, size);

        return ApiResponse.success(response);
    }
}
