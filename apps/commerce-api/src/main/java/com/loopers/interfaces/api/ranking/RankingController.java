package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingFacade.RankingResult;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.ranking.RankingDto.RankingResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rankings")
@Validated
public class RankingController implements RankingApiSpec {

  private final RankingFacade rankingFacade;

  @Override
  @GetMapping("/daily")
  public ApiResponse<RankingResponse> getDailyRanking(
      @RequestHeader(value = ApiHeaders.USER_ID, required = false) Long userId,
      @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    RankingResult result = rankingFacade.getDailyRanking(date, page, size, userId);
    RankingResponse response = RankingResponse.from(result);
    return ApiResponse.success(response);
  }
}
