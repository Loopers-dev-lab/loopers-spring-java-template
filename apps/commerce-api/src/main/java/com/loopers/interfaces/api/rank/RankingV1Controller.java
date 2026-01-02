package com.loopers.interfaces.api.rank;

import com.loopers.application.rank.RankingFacade;
import com.loopers.application.rank.RankingInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.rank.RankingV1Dto.RankingResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
  public ApiResponse<List<RankingResponse>> getRankings(
      @RequestParam(value = "type", defaultValue = "DAILY") String type,
      @RequestParam(value = "date") String date,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "size", defaultValue = "20") int size
  ) {
    List<RankingInfo> infos = rankingFacade.getRankings(type, date, page, size);
    List<RankingV1Dto.RankingResponse> response = infos.stream()
        .map(RankingV1Dto.RankingResponse::from)
        .toList();

    return ApiResponse.success(response);
  }
}
