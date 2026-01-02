package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo.RankingPageInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.ranking.RankingV1Dto.RankingPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class RankingV1Controller {

    private final RankingFacade rankingFacade;

    /**
     * 랭킹 페이지 조회
     * GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1
     *
     * @param page 페이지 번호 (1-based, 기본값 1)
     */
    @GetMapping("/rankings")
    public ApiResponse<RankingPageResponse> getRankings(
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        // API는 1-based, 내부는 0-based로 변환
        int zeroBasedPage = Math.max(0, page - 1);
        RankingPageInfo info = rankingFacade.getRankings(date, zeroBasedPage, size);
        RankingPageResponse response = RankingPageResponse.from(info);
        return ApiResponse.success(response);
    }
}
