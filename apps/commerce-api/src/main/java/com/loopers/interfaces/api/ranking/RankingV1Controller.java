package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingCommand;
import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.interfaces.api.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;

    @GetMapping
    @Override
    public ApiResponse<RankingV1Dto.DailyRankingListResponse> getDailyRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        RankingCommand.GetDailyRankingCommand command = new RankingCommand.GetDailyRankingCommand(
                date,
                page,
                size
        );

        RankingInfo rankingInfo = rankingFacade.getDailyRanking(command);
        RankingV1Dto.DailyRankingListResponse response = RankingV1Dto.DailyRankingListResponse.from(rankingInfo);

        return ApiResponse.success(response);
    }

}
