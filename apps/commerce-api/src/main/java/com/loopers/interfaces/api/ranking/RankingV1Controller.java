package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingCommand;
import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
    public ApiResponse<RankingV1Dto.RankingListResponse> getRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        RankingCommand.RankingType rankingType;
        try {
            rankingType = RankingCommand.RankingType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 랭킹 타입입니다.");
        }

        RankingCommand.GetRankingCommand command = new RankingCommand.GetRankingCommand(date, rankingType, page, size);

        RankingInfo rankingInfo = rankingFacade.getRanking(command);
        RankingV1Dto.RankingListResponse response = RankingV1Dto.RankingListResponse.from(rankingInfo);

        return ApiResponse.success(response);
    }

}
