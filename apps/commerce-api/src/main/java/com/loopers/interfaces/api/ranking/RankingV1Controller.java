package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingCommand;
import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingPageInfo;
import com.loopers.domain.ranking.RankingInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ApiResponse<RankingV1Dto.RankingPageResponse> getRankings(String date, int page, int size) {
        RankingCommand command = RankingCommand.of(date, page, size);
        RankingPageInfo pageInfo = rankingFacade.getRankingPage(command);
        return ApiResponse.success(RankingV1Dto.RankingPageResponse.from(pageInfo));
    }

    @Override
    public ApiResponse<RankingV1Dto.TopNResponse> getTopN(String date, int n) {
        LocalDate targetDate = parseDate(date);
        List<RankingInfo> rankings = rankingFacade.getTopN(targetDate, n);
        return ApiResponse.success(RankingV1Dto.TopNResponse.of(rankings, targetDate));
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(date, DATE_FORMATTER);
    }
}
