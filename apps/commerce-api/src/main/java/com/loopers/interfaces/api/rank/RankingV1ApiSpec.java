package com.loopers.interfaces.api.rank;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.rank.RankingV1Dto.RankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Ranking API", description = "실시간 상품 랭킹 관련 API 입니다.")
public interface RankingV1ApiSpec {

  @Operation(summary = "실시간 랭킹 조회", description = "특정 날짜의 인기 상품 랭킹을 조회합니다.")
  ApiResponse<List<RankingResponse>> getRankings(
      @Parameter(description = "랭킹 타입 (DAILY, WEEKLY, MONTHLY)", example = "WEEKLY") String type,
      @Parameter(description = "조회 날짜 (yyyyMMdd)", example = "20251225") String date,
      @Parameter(description = "페이지 번호") int page,
      @Parameter(description = "페이지 크기") int size
  );
}
