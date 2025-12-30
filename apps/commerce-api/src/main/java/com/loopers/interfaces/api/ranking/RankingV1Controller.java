package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;

    @Override
    @GetMapping
    public ApiResponse<RankingV1Dto.RankingResponse> getRankings(String date, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        long total = rankingFacade.count(date);
        return ApiResponse.success(RankingV1Dto.RankingResponse.from(
                rankingFacade.getRankingItems(date, safePage, safeSize),
                safePage,
                safeSize,
                total
        ));
    }
}
